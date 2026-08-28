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
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import java.util.Calendar
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

        /** Miroir natif de JS_CUSTOM.ucAzanVoiceEnabledFajr/Dohr/Assr/Mgrb/Isha
         *  (custom.js, modale "تفعيل الأذان حسب الصلاة" -- accessible seulement
         *  quand PREF_VOICE_MODE ci-dessus est actif). Combiné en ET avec
         *  PREF_VOICE_MODE dans onStartCommand : celui-ci reste le garde-fou
         *  global (désactivé, aucune prière ne peut jouer le vrai son quel que
         *  soit ce réglage) ; ces flags permettent en plus de couper la voix
         *  d'une prière précise sans toucher aux autres. Vrai par défaut
         *  (getBoolean(..., true)) tant que la modale n'a jamais été ouverte. */
        const val PREF_VOICE_FAJR    = "voice_mode_fajr"
        const val PREF_VOICE_DHUHR   = "voice_mode_dhuhr"
        const val PREF_VOICE_ASR     = "voice_mode_asr"
        const val PREF_VOICE_MAGHREB = "voice_mode_maghreb"
        const val PREF_VOICE_ISHA    = "voice_mode_isha"

        private fun perPrayerVoicePrefKey(prayer: String): String? = when (prayer) {
            "Fajr"    -> PREF_VOICE_FAJR
            "Dhuhr"   -> PREF_VOICE_DHUHR
            "Asr"     -> PREF_VOICE_ASR
            "Maghreb" -> PREF_VOICE_MAGHREB
            "Isha"    -> PREF_VOICE_ISHA
            else      -> null
        }

        /** Miroir natif de JS_DATA.ucActivateJomoaAzan (case "أذان الجمعة") et de
         *  l'heure de Jumu'a effectivement retenue (prayerTimesMinutesObject.DOHR
         *  le vendredi, en minutes depuis minuit) -- synchronise a chaque
         *  reprogrammation d'alarme (cf. MobileJsBridge.syncJumuaAzanState,
         *  appele depuis custom.js _sendToNative). Sert au garde-fou vendredi de
         *  onStartCommand : le vendredi, la SEULE lecture audio autorisee est
         *  l'azan de Jumu'a a son heure planifiee. Toute alarme "Dhuhr" qui
         *  sonne le vendredi a une autre heure (typiquement l'heure du Dhuhr
         *  ordinaire ~12:25, laissee par une alarme programmee un jour ou
         *  isFriday etait faux -- fallback "heure deja passee -> demain" de
         *  scheduleSinglePrayer) est ignoree ici, sans dependre de la
         *  reprogrammation quotidienne cote JS (qui peut ne pas encore etre
         *  passee, ou l'appli etre restee fermee).
         *  Defaut PREF_JUMUA_AZAN_ENABLED = false : si jamais synchronise, on
         *  n'ouvre pas de son de Jumu'a par defaut (sens conservateur). */
        const val PREF_JUMUA_AZAN_ENABLED  = "jumua_azan_enabled"
        const val PREF_JUMUA_TIME_MINUTES  = "jumua_time_minutes"

        /** Id (catalogue azan-catalog.json) du muezzin personnalisé choisi par
         *  l'utilisateur (custom.js, _installAzanCatalogFeature), synchronise
         *  via MobileJsBridge.syncAzanCatalogSelection à chaque selection/
         *  deselection ET a chaque reprogrammation d'alarme (memes garanties
         *  que PREF_VOICE_MODE/PREF_SHORT_AZAN ci-dessus : relu ici, pas
         *  seulement au moment ou l'alarme a ete programmee). Chaine vide =
         *  aucune selection -> son par defaut de l'appli (fichiers spec/audio). */
        const val PREF_AZAN_FAJR_ID    = "azan_catalog_fajr_id"
        const val PREF_AZAN_GENERAL_ID = "azan_catalog_general_id"

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

    /** Vrai si on est vendredi (fuseau/horloge locale de l'appareil) -- utilise
     *  par le garde-fou Jumu'a de onStartCommand. */
    private fun isFridayNow(): Boolean =
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

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
        // ET avec le flag par prière (cf. PREF_VOICE_FAJR/... ci-dessus) : le
        // switch global reste prioritaire (désactivé -> jamais de voix, quel
        // que soit ce flag), une prière décochée dans la modale coupe la voix
        // pour elle seule sans affecter les autres.
        val perPrayerVoiceEnabled = perPrayerVoicePrefKey(prayer)?.let { prefs.getBoolean(it, true) } ?: true
        val voiceMode = prefs.getBoolean(PREF_VOICE_MODE, intentVoiceMode) && perPrayerVoiceEnabled
        val fullAudioMode = voiceMode && !shortAzan

        // ── Garde-fou VENDREDI ────────────────────────────────────────────────
        // Le vendredi, la seule lecture audio autorisee est l'azan de la Jumu'a,
        // et uniquement a son heure planifiee. Une alarme "Dhuhr" qui sonne un
        // vendredi est soit cet azan de Jumu'a legitime, soit une alarme
        // PERIMEE : programmee un jour ou isFriday etait faux (donc sans
        // substitution de l'heure Jumu'a ni prise en compte de la case "أذان
        // الجمعة"), typiquement via le fallback "heure deja passee -> demain" de
        // MobileJsBridge.scheduleSinglePrayer, qui la fait retomber le vendredi
        // a l'heure du Dhuhr ORDINAIRE (~12:25) et non a l'heure de la Jumu'a
        // (~13:15). Incident declencheur : mosquee tn.monastir.aboubakr,
        // vendredi 28/08/2026 -- azan complet joue nativement a 12:25 PENDANT la
        // recitation du Coran d'avant-Jumu'a (auto-demarree a 12:00, coupee a
        // 12:44), alors que la case Jumu'a etait decochee. custom.js desarme
        // bien cette alarme a sa reprogrammation quotidienne, mais celle-ci peut
        // ne pas encore etre passee (ou l'appli etre restee fermee) -- d'ou ce
        // controle natif, evalue au tout dernier moment avant lecture.
        if (prayer == "Dhuhr" && isFridayNow()) {
            val jumuaEnabled = prefs.getBoolean(PREF_JUMUA_AZAN_ENABLED, false)
            val jumuaMinutes = prefs.getInt(PREF_JUMUA_TIME_MINUTES, -1)
            val firedMinutes = prayerHour * 60 + prayerMinute
            // Tolerance 2 min : ecart d'arrondi/reglage athan entre l'heure
            // figee dans l'alarme et l'heure Jumu'a courante. Si l'heure Jumu'a
            // n'a jamais ete synchronisee (-1), on ne bloque que sur le flag.
            val timeMatches = jumuaMinutes < 0 || kotlin.math.abs(firedMinutes - jumuaMinutes) <= 2
            if (!jumuaEnabled || !timeMatches) {
                NativeEventLog.log(
                    this, "AZAN",
                    "NATIVE_SKIP_JUMUA prayer=Dhuhr jumuaEnabled=$jumuaEnabled " +
                        "firedMin=$firedMinutes jumuaMin=$jumuaMinutes"
                )
                stopSelfCleanly()
                return START_NOT_STICKY
            }
        }

        // ── Garde-fou COUPURE MANUELLE (boitier mosquee, appli au 1er plan) ───
        // Sur un boitier de mosquee l'appli est en permanence au premier plan et
        // l'azan "voix complete" est joue ICI (natif) meme au premier plan. On
        // le route alors sur STREAM_MUSIC (cf. playAzan, USAGE_MEDIA quand
        // foreground) pour qu'il suive le volume que le responsable regle a la
        // telecommande. Corollaire : s'il a mis ce volume a 0 (coupure
        // deliberee -- maintenance, circonstance particuliere...), l'azan ne
        // doit PAS sortir malgre lui. Sans ce controle, l'azan sortait quand
        // meme a fond car il partait en USAGE_ALARM sur STREAM_ALARM, un flux
        // que la telecommande ne touche pas et que le boost pre-azan (T-2min)
        // avait mis au maximum (incident Maghreb 27/08/2026, mosquee
        // tn.monastir.aboubakr : volume baisse pendant fin.ogg, azan a fond).
        // Appli en arriere-plan (telephone, ou reglages ouverts) : comportement
        // inchange -- USAGE_ALARM, la lecture doit avoir lieu pour ne pas rater
        // la priere.
        if (MainActivity.isAppInForeground) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                NativeEventLog.log(this, "AZAN", "NATIVE_SKIP_MUTED prayer=$prayer stream=music")
                stopSelfCleanly()
                return START_NOT_STICKY
            }
        }

        // Azan court / mode bip au premier plan : deja joue par le WebView
        // (custom.js, playAzanSoundFunction) via son propre <audio>, non
        // coupe dans ce mode (seul le mode "voix complete" mute le <audio>
        // JS, cf. custom.js AUDIO_SOURCE) -- jouer aussi nativement ferait un
        // double son. On saute donc uniquement ce cas precis ; en
        // arriere-plan (WebView en pause, rien ne joue cote JS), ce service
        // doit jouer quelque chose : cf. playAzan ci-dessous, qui choisit
        // desormais le bon fichier (bip/court/complet) au lieu de toujours
        // retomber sur l'azan complet (bug constate en pratique, rapport
        // debug 24/07/2026 : acEnableSwitch=false mais azan complet joue en
        // arriere-plan).
        if (!fullAudioMode && MainActivity.isAppInForeground) {
            Log.d("TWKT", "AzanPlaybackService: app in foreground (bip/short azan handled by WebView), skipping native playback")
            NativeEventLog.log(this, "AZAN", "NATIVE_SKIP_FOREGROUND_LEGACY prayer=$prayer shortAzan=$shortAzan voiceMode=$voiceMode")
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        NativeEventLog.log(this, "AZAN", "NATIVE_PLAY_START prayer=$prayer fullAudioMode=$fullAudioMode " +
            "foreground=${MainActivity.isAppInForeground} shortAzan=$shortAzan voiceMode=$voiceMode")
        isPlayingNow = true
        acquireWakeLock()
        playAzan(prayer == "Fajr", voiceMode, shortAzan)
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

    private fun playAzan(isFajr: Boolean, voiceMode: Boolean, shortAzan: Boolean) {
        // Empeche tout chevauchement : si un MediaPlayer est deja en cours
        // (nouvel appel a onStartCommand pendant qu'une lecture precedente
        // tourne encore -- constate en pratique 11/08/2026, boitier KM22,
        // rapport debug : 5 alarmes prieres livrees par AlarmManager a la
        // MEME seconde apres un reveil/redemarrage, tres probablement une
        // correction d'horloge systeme post-demarrage qui a rendu plusieurs
        // alarmes "en retard" d'un coup), l'arreter/liberer AVANT d'en creer
        // un nouveau. Sans ce garde-fou, l'ancien lecteur devient orphelin
        // (la reference mediaPlayer est ecrasee) et continue de jouer jusqu'a
        // la fin de son fichier sans qu'aucun mecanisme d'arret (notification,
        // popup, switch catalogue -- tous n'agissent que sur la reference
        // COURANTE) ne puisse plus l'atteindre : plusieurs azans se
        // superposaient, seul un kill complet du process (reload/redemarrage
        // de l'appli, qui tue tous les MediaPlayer avec lui) les arretait.
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("TWKT", "AzanPlaybackService: previous player cleanup error: ${e.message}")
        }
        mediaPlayer = null

        try {
            // Reproduit exactement le choix de fichier de playAzanSoundFunction()
            // (m2body.js) : bip si le mode vocal est desactive (ucAzanIqamaByVoice
            // != 1), azan court si actif (ucShortAzanActive == 1), sinon l'azan
            // complet -- ces 2 premiers sont des fichiers CORE fixes (hors spec/,
            // jamais personnalises par mosquee, memes chemins que index.html),
            // contrairement a azan_fajr.ogg/azan.ogg qui restent personnalisables
            // par mosquee (spec/audio/).
            val assetPath = when {
                !voiceMode -> "audio/wbeeep.mp3"
                shortAzan  -> "audio/short_azan.mp3"
                isFajr     -> "spec/audio/azan_fajr.ogg"
                else       -> "spec/audio/azan.ogg"
            }

            // Muezzin personnalise choisi dans le catalogue (custom.js,
            // _acSelectCommit -> MobileJsBridge.syncAzanCatalogSelection) :
            // uniquement en mode "voix complete" (court/bip gardent leur son
            // fixe, jamais couverts par le catalogue cote JS non plus, cf.
            // _acApplyAzanToPlayer qui ne touche que audioFajrElement/
            // audioAzanElement). Repli silencieux sur le fichier bundle si
            // aucune selection, id introuvable sur disque, ou fichier efface
            // depuis (ex. stockage libere manuellement).
            var customFile: java.io.File? = null
            if (voiceMode && !shortAzan) {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val selectedId = prefs.getString(
                    if (isFajr) PREF_AZAN_FAJR_ID else PREF_AZAN_GENERAL_ID, ""
                ).orEmpty()
                if (selectedId.isNotEmpty()) {
                    val f = AzanCatalogManager.getInstalledFile(this, selectedId)
                    if (f != null && f.exists()) customFile = f
                }
            }

            // Appli au premier plan (cas normal du boitier mosquee) : on joue
            // sur STREAM_MUSIC (USAGE_MEDIA) pour que l'azan suive le volume que
            // le responsable regle a la telecommande -- une baisse manuelle est
            // alors reellement respectee (cf. garde-fou NATIVE_SKIP_MUTED dans
            // onStartCommand). Appli en arriere-plan (telephone, appli fermee) :
            // USAGE_ALARM comme avant, pour percer le mode silencieux/DND et ne
            // pas rater la priere.
            val foreground = MainActivity.isAppInForeground
            val usage = if (foreground) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ALARM
            NativeEventLog.log(
                this@AzanPlaybackService, "AZAN",
                "NATIVE_AUDIO_ROUTE usage=" + (if (foreground) "media" else "alarm")
            )
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (customFile != null) {
                    Log.d("TWKT", "AzanPlaybackService: using custom catalog azan file=${customFile.name}")
                    NativeEventLog.log(this@AzanPlaybackService, "AZAN", "NATIVE_PLAY_CUSTOM_CATALOG file=${customFile.name}")
                    setDataSource(customFile.absolutePath)
                } else {
                    val afd = assets.openFd(assetPath)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }
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
