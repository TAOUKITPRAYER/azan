package net.tawkit.mobile

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Joue l'azan reellement (fichier OGG bundle dans les assets, via MediaPlayer
 * natif) quand PrayerAlarmReceiver recoit une alarme AlarmManager pour l'heure
 * exacte de la priere (minutesBefore == 0). Necessaire car le <audio> HTML du
 * WebView (custom.js) ne joue que quand l'appli est au premier plan.
 *
 * Foreground service (type mediaPlayback) : garde le processus vivant le
 * temps de la lecture, meme appli fermee/ecran eteint. Poste elle-meme
 * l'unique notification tappable de l'azan (tap -> ouvre MainActivity, bouton
 * "Arreter" -> ACTION_STOP) ; PrayerAlarmReceiver ne poste plus de notification
 * separee pour ce cas (minutesBefore == 0), pour eviter d'en avoir deux.
 */
class AzanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var sensorManager: SensorManager? = null
    private var flipListener: SensorEventListener? = null
    private var flipConsecutiveCount = 0

    companion object {
        private const val NOTIF_ID = 9001
        // Securite : coupe la lecture/le wake lock si onCompletion/onError ne
        // se declenchent jamais (fichier corrompu, etc.) pour ne pas garder
        // le CPU eveille indefiniment.
        private const val MAX_WAKE_LOCK_MS = 6 * 60 * 1000L

        const val ACTION_STOP = "net.tawkit.mobile.action.STOP_AZAN"

        /** SharedPreferences partagees avec MobileJsBridge.setFlipToMuteEnabled
         *  (onglet الإعدادات — custom.js, _ucToggleFlipToMuteAzan). */
        const val PREFS_NAME = "tawkit_azan_prefs"
        const val PREF_FLIP_TO_MUTE = "flip_to_mute_enabled"

        /** Miroir de JS_DATA.ucAzanIqamaByVoice / ucShortAzanActive, synchronise
         *  a chaque saveSettingsToStorageFunction() (cf. MobileJsBridge.
         *  syncAzanPlaybackFlags) -- relu ICI (onStartCommand), pas seulement
         *  depuis les extras de l'intent (potentiellement programmes des
         *  heures plus tot), pour garantir qu'un azan desactive ne peut
         *  jamais jouer le vrai son nativement. */
        const val PREF_VOICE_MODE  = "voice_mode_enabled"
        const val PREF_SHORT_AZAN  = "short_azan_active"

        /** True pendant toute lecture reelle en cours (entre le debut de
         *  playAzan() et stopSelfCleanly()/onDestroy()) -- interroge par
         *  MobileJsBridge.isAzanCurrentlyPlaying(), lu par custom.js
         *  (_ucResyncPrayerSequence) AVANT d'appeler hideAzanPopupFunction()
         *  (qui declenche justement l'arret natif) pour savoir si CE resync
         *  vient d'interrompre un azan reellement sonore -- et donc s'il faut
         *  enchainer sur un reload automatique (cf. custom.js) pour au moins
         *  rattraper le countdown iqama, plutot que de laisser l'utilisateur
         *  sur la page principale sans rien. */
        @Volatile var isPlayingNow: Boolean = false

        // Detection "telephone pose ecran contre la table" : la gravite pointe
        // alors vers l'ecran, donnant un z d'accelerometre proche de -9.8 au
        // lieu de +9.8 (pose a l'endroit) ou proche de 0 (a la verticale).
        private const val FLIP_Z_THRESHOLD = -7f
        // Nombre de lectures consecutives sous le seuil avant de couper --
        // evite qu'un soubresaut bref (manipulation, pose rapide) ne coupe
        // l'azan par erreur.
        private const val FLIP_CONFIRM_COUNT = 3

        private val ARABIC_NAMES = mapOf(
            "Fajr"    to "الفجر",
            "Dhuhr"   to "الظهر",
            "Asr"     to "العصر",
            "Maghreb" to "المغرب",
            "Isha"    to "العشاء",
            "Jumua"   to "الجمعة"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayer = intent?.getStringExtra("prayer") ?: "Salat"

        if (intent?.action == ACTION_STOP) {
            // source : "notification_button" (bouton "Arreter", cf. buildNotification
            // ci-dessous) ou "webview_popup_close" (MobileJsBridge.stopAzanPlayback,
            // appele depuis custom.js quand l'utilisateur ferme la popup dans l'appli).
            // Absent (vieux client / appel direct) -> "unknown".
            val source = intent.getStringExtra("source") ?: "unknown"
            Log.d("TWKT", "AzanPlaybackService: stop requested by user (source=$source)")
            NativeEventLog.log(this, "AZAN", "NATIVE_STOP_USER prayer=$prayer source=$source")
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        // Mode "voix complete" (ni azan court ni bip, cf. shortAzan/voiceMode
        // ci-dessous) : source audio UNIQUE desormais -- ce service joue
        // TOUJOURS le son reel, premier plan ou arriere-plan, sans se soucier
        // de MainActivity.isAppInForeground. Le WebView (custom.js) coupe de
        // son cote son propre <audio> correspondant (muted) pour ne garder
        // que cette seule sortie audible ; il continue de tourner en silence
        // pour piloter l'UI (equalizer, fermeture auto de la popup). Ancien
        // bug corrige au passage : sortir l'appli PENDANT la lecture ne
        // coupait plus rien cote natif (la garde etait evaluee une seule
        // fois, au moment de l'alarme) -- desormais la lecture continue sans
        // interruption a travers les changements d'etat foreground/background.
        // Extras de l'intent = valeur au moment de LA PROGRAMMATION (peut
        // dater de plusieurs heures, cf. reprogrammation quotidienne). Le
        // SharedPreferences (cf. PREF_VOICE_MODE/PREF_SHORT_AZAN, synchronise
        // a CHAQUE sauvegarde de reglage cote JS, quel que soit le chemin
        // emprunte) est relu ICI, au tout dernier moment avant de jouer, et
        // l'EMPORTE s'il existe -- garantit qu'un azan desactive APRES la
        // programmation de l'alarme mais AVANT qu'elle sonne ne joue jamais
        // le vrai son. Repli sur l'extra de l'intent seulement si ce
        // SharedPreferences n'a encore jamais ete synchronise (tout premier
        // lancement, avant la premiere sauvegarde de reglage).
        val prayerHour   = intent?.getIntExtra("prayerHour", 0) ?: 0
        val prayerMinute = intent?.getIntExtra("prayerMinute", 0) ?: 0

        // OBLIGATOIRE avant toute autre logique : PrayerAlarmReceiver demarre
        // ce service via startForegroundService(), ce qui impose a Android de
        // recevoir un startForeground() quasi immediatement (sinon
        // ForegroundServiceDidNotStartInTimeException -> crash immediat de
        // l'appli, meme si le popup azan etait ouvert au premier plan --
        // constate en pratique via rapport debug + logcat le 23/07/2026,
        // exactement sur le cas "azan court + appli au premier plan"
        // ci-dessous, qui faisait un stopSelf() precoce SANS jamais appeler
        // startForeground()). On la poste donc tout de suite, quelle que soit
        // la decision de lecture prise ensuite, et on la retire proprement
        // (stopForeground, cf. stopSelfCleanly) si on decide finalement de ne
        // pas jouer.
        MobileJsBridge.createNotificationChannel(this)
        startForeground(NOTIF_ID, buildNotification(prayer, prayerHour, prayerMinute))

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val intentShortAzan = intent?.getBooleanExtra("shortAzan", false) ?: false
        val intentVoiceMode = intent?.getBooleanExtra("voiceMode", true) ?: true
        val shortAzan = prefs.getBoolean(PREF_SHORT_AZAN, intentShortAzan)
        val voiceMode = prefs.getBoolean(PREF_VOICE_MODE, intentVoiceMode)
        val fullAudioMode = voiceMode && !shortAzan

        // Azan court / mode bip : non geres nativement pour l'instant -- on
        // conserve l'ancien comportement (JS premier plan uniquement, natif
        // arriere-plan uniquement) plutot que de jouer le mauvais fichier
        // (ce service ne connait que l'azan complet, cf. playAzan ci-dessous).
        if (!fullAudioMode && MainActivity.isAppInForeground) {
            Log.d("TWKT", "AzanPlaybackService: app in foreground (legacy short/beep mode), skipping native playback")
            NativeEventLog.log(this, "AZAN", "NATIVE_SKIP_FOREGROUND_LEGACY prayer=$prayer shortAzan=$shortAzan voiceMode=$voiceMode")
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        NativeEventLog.log(this, "AZAN", "NATIVE_PLAY_START prayer=$prayer fullAudioMode=$fullAudioMode " +
            "foreground=${MainActivity.isAppInForeground} shortAzan=$shortAzan voiceMode=$voiceMode")
        isPlayingNow = true
        acquireWakeLock()
        playAzan(prayer == "Fajr")
        maybeStartFlipToMuteDetection()
        return START_NOT_STICKY
    }

    private fun buildNotification(prayer: String, prayerHour: Int, prayerMinute: Int): Notification {
        val arabicName = ARABIC_NAMES[prayer] ?: prayer
        val azanTime = String.format("%02d:%02d", prayerHour, prayerMinute)

        // Tap sur la notification -> ouvre l'app (meme intent/flags que
        // PrayerAlarmReceiver.showNotification pour les rappels).
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            action = "net.tawkit.mobile.OPEN_PRAYER"
            putExtra("prayer", prayer)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            this, prayer.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Bouton "Arreter" -> redemarre ce meme service avec ACTION_STOP
        // (intent explicite, pas besoin d'intent-filter dans le manifest).
        val stopIntent = Intent(this, AzanPlaybackService::class.java).apply {
            action = ACTION_STOP
            putExtra("prayer", prayer)
            putExtra("source", "notification_button")
        }
        val pendingStop = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MobileJsBridge.AZAN_PLAYBACK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$prayer — $arabicName")
            .setContentText("Il est $azanTime — حي على الصلاة")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)   // le son reel est joue par MediaPlayer, pas la notification
            .setContentIntent(pendingTap)
            .addAction(android.R.drawable.ic_media_pause, "Arrêter", pendingStop)
            .build()
    }

    /**
     * Enregistre un ecouteur accelerometre le temps de la lecture uniquement
     * (pas en continu en arriere-plan) si l'utilisateur a active le reglage
     * "couper l'azan en retournant le telephone". Coupe la lecture des que le
     * telephone est detecte ecran contre la table de facon soutenue.
     */
    private fun maybeStartFlipToMuteDetection() {
        val enabled = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_FLIP_TO_MUTE, false)
        if (!enabled) return

        val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        flipConsecutiveCount = 0
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values[2] < FLIP_Z_THRESHOLD) {
                    flipConsecutiveCount++
                    if (flipConsecutiveCount >= FLIP_CONFIRM_COUNT) {
                        Log.d("TWKT", "AzanPlaybackService: flip-to-mute triggered")
                        NativeEventLog.log(this@AzanPlaybackService, "AZAN", "NATIVE_STOP_FLIP_TO_MUTE")
                        stopSelfCleanly()
                    }
                } else {
                    flipConsecutiveCount = 0
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager = sm
        flipListener = listener
        sm.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopFlipToMuteDetection() {
        flipListener?.let { sensorManager?.unregisterListener(it) }
        flipListener = null
        sensorManager = null
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tawkit:AzanPlayback").apply {
            acquire(MAX_WAKE_LOCK_MS)
        }
    }

    private fun playAzan(isFajr: Boolean) {
        try {
            val assetPath = "spec/audio/" + if (isFajr) "audio_fajr.ogg" else "audio_azan.ogg"
            val afd = assets.openFd(assetPath)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    Log.d("TWKT", "AzanPlaybackService: playback completed")
                    NativeEventLog.log(this@AzanPlaybackService, "AZAN", "NATIVE_PLAY_END_COMPLETED")
                    stopSelfCleanly()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("TWKT", "AzanPlaybackService: MediaPlayer error what=$what extra=$extra")
                    NativeEventLog.log(this@AzanPlaybackService, "AZAN", "NATIVE_PLAY_END_ERROR what=$what extra=$extra")
                    stopSelfCleanly()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("TWKT", "AzanPlaybackService: playAzan error: ${e.message}")
            stopSelfCleanly()
        }
    }

    private fun stopSelfCleanly() {
        isPlayingNow = false
        stopFlipToMuteDetection()
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("TWKT", "AzanPlaybackService: release error: ${e.message}")
        }
        mediaPlayer = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopSelfCleanly()
        super.onDestroy()
    }
}
