package net.tawkit.mobile

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.webkit.JavascriptInterface
import com.onesignal.OneSignal
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * JavaScript Bridge for the Mobile app.
 *
 * Usage from custom.js (or mosquee.js):
 *
 *   // Schedule a notification for Fajr at 05:30 tomorrow
 *   if (window.AndroidMobile) {
 *       window.AndroidMobile.schedulePrayerNotifications(JSON.stringify([
 *           { prayer: "Fajr",    hour: 5,  minute: 30 },
 *           { prayer: "Dhuhr",   hour: 12, minute: 45 },
 *           { prayer: "Asr",     hour: 16, minute: 10 },
 *           { prayer: "Maghreb", hour: 19, minute: 52 },
 *           { prayer: "Isha",    hour: 21, minute: 20 }
 *       ]));
 *   }
 *
 *   // Cancel all scheduled notifications
 *   if (window.AndroidMobile) window.AndroidMobile.cancelAllNotifications();
 *
 *   // Check if running inside Android app
 *   var isAndroid = (typeof window.AndroidMobile !== 'undefined');
 */
class MobileJsBridge(
    private val context: Context,
    /** Declenche le picker SAF (ActivityResultContracts.OpenDocumentTree) depuis MainActivity. */
    private val onRequestImport: () -> Unit = {},
    /** Active/desactive FLAG_KEEP_SCREEN_ON sur la fenetre (cf. MainActivity.setKeepScreenOn). */
    private val onSetKeepScreenOn: (Boolean) -> Unit = {},
    /** Lance à la demande la vérification de version GitHub. */
    private val onCheckForUpdate: () -> Unit = {},
    /** Ouvre le menu outils TV (reglages Android / changer l'ecran d'accueil)
     *  — cf. MainActivity.showTvUtilityMenu(). Pas d'ecran de reglages
     *  accessible en mode horizontal pour proposer ces actions autrement. */
    private val onOpenTvUtilityMenu: () -> Unit = {}
) {

    companion object {
        // Suffixe v2 volontaire : les propriétés sonores d'un canal Android
        // existant sont immuables. Un nouvel identifiant réactive donc le son
        // par défaut pour les installations qui avaient reçu l'ancien canal.
        const val CHANNEL_ID = "tawkit_prayer_events_v2"
        const val CHANNEL_NAME = "Alertes azan et mode silencieux"

        /** Canal discret (pas de son/vibration) : sert uniquement a maintenir le
         *  telechargement de recitateur en foreground service (cf.
         *  ReciterDownloadWorker.buildForegroundInfo) pour qu'il survive au
         *  passage de l'appli en arriere-plan. */
        const val DOWNLOAD_CHANNEL_ID = "tawkit_download_channel"
        const val DOWNLOAD_CHANNEL_NAME = "Telechargement des recitateurs"

        /** Canal discret (pas de son/vibration) : notification "ongoing" du
         *  foreground service AzanPlaybackService pendant la lecture native
         *  de l'azan. Doit rester silencieux : le son est celui de l'azan
         *  joue par MediaPlayer, pas celui de la notification. */
        const val AZAN_PLAYBACK_CHANNEL_ID = "tawkit_azan_playback_channel"
        const val AZAN_PLAYBACK_CHANNEL_NAME = "Lecture de l'azan"

        /** Index stable par priere, utilise pour deriver un requestCode de
         *  PendingIntent deterministe (cf. requestCodeFor ci-dessous) -
         *  independant de la position de l'entree dans le tableau JSON envoye
         *  par custom.js (qui peut contenir 1 ou 2 entrees par priere selon
         *  que l'alerte "N min avant" est activee). */
        private val PRAYER_INDEX = mapOf(
            "Fajr" to 0, "Dhuhr" to 1, "Asr" to 2, "Maghreb" to 3, "Isha" to 4, "Jumua" to 5
        )

        /** requestCode fixe par (priere, alerte-avant/heure-exacte) : evite les
         *  collisions/desynchronisations entre schedulePrayerNotifications()
         *  (qui programme) et cancelAllNotifications() (qui doit annuler
         *  exactement les memes PendingIntent). */
        private fun requestCodeFor(prayer: String, minutesBefore: Int): Int {
            val idx = PRAYER_INDEX[prayer] ?: 6
            return 100 + idx * 2 + (if (minutesBefore > 0) 1 else 0)
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerte avant azan, mise en muet et reprise de la sonnerie"
                    setSound(
                        Settings.System.DEFAULT_NOTIFICATION_URI,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    enableVibration(true)
                    enableLights(true)
                }
                val downloadChannel = NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    DOWNLOAD_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Progression du telechargement des recitateurs Coran"
                }
                val azanPlaybackChannel = NotificationChannel(
                    AZAN_PLAYBACK_CHANNEL_ID,
                    AZAN_PLAYBACK_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notification silencieuse pendant la lecture native de l'azan"
                    setSound(null, null)
                    enableVibration(false)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
                nm.createNotificationChannel(downloadChannel)
                nm.createNotificationChannel(azanPlaybackChannel)
            }
        }
    }

    init {
        createNotificationChannel(context)
    }

    /**
     * Historique persistant du cycle de vie de l'azan (cf. NativeEventLog) --
     * appele depuis custom.js (console de debug) pour fusionner les
     * evenements natifs (survenus meme appli fermee) avec les logs JS.
     */
    @JavascriptInterface
    fun getNativeEventLog(): String {
        return NativeEventLog.getAllAsJson(context)
    }

    @JavascriptInterface
    fun clearNativeEventLog() {
        NativeEventLog.clear(context)
    }

    /**
     * Schedule prayer notifications for today.
     * Called from custom.js after prayer times are calculated.
     *
     * @param jsonArray JSON array of { prayer, hour, minute } objects
     */
    @JavascriptInterface
    fun schedulePrayerNotifications(jsonArray: String) {
        try {
            val prayers = JSONArray(jsonArray)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (!canScheduleExactAlarms()) {
                Log.w("TWKT", "schedulePrayerNotifications: exact alarm permission NOT granted - alarms will fail")
            }

            for (i in 0 until prayers.length()) {
                val obj = prayers.getJSONObject(i)
                val prayer       = obj.getString("prayer")
                val hour         = obj.getInt("hour")          // heure alarme (azan - minutesBefore)
                val minute       = obj.getInt("minute")
                val prayerHour   = obj.optInt("prayerHour",   hour)    // heure reelle azan
                val prayerMinute = obj.optInt("prayerMinute", minute)
                val minutesBefore = obj.optInt("minutesBefore", 0)

                scheduleSinglePrayer(alarmManager, prayer, hour, minute, prayerHour, prayerMinute, minutesBefore)
            }
            Log.d("TWKT", "Scheduled ${prayers.length()} prayer alerts")
        } catch (e: Exception) {
            Log.e("TWKT", "schedulePrayerNotifications error: ${e.message}")
        }
    }

    private fun scheduleSinglePrayer(
        alarmManager: AlarmManager,
        prayer: String,
        hour: Int,
        minute: Int,
        prayerHour: Int,
        prayerMinute: Int,
        minutesBefore: Int
    ) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Si l'heure calculee est deja passee aujourd'hui, programmer pour
            // demain a la meme heure plutot que d'abandonner. AVANT ce correctif,
            // toute priere deja passee "aujourd'hui" au moment de l'appel
            // (typiquement TOUTES les prieres si l'appli est rouverte le soir,
            // Fajr y compris des le lendemain matin) n'etait tout simplement
            // jamais programmee tant que l'appli ne se rouvrait pas -- c'est ce
            // qui causait azan/notification totalement absents du jour au
            // lendemain. Le decalage d'un jour reutilise l'heure du jour meme
            // (calcul le plus recent disponible) : ecart de 1-2 min au pire
            // avec l'heure exacte du lendemain, corrige de toute facon des la
            // prochaine reprogrammation quotidienne (custom.js, UC_EVT.AZAN_TIME).
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer", prayer)
            putExtra("hour", hour)
            putExtra("minute", minute)
            putExtra("prayerHour", prayerHour)       // heure réelle de l'azan
            putExtra("prayerMinute", prayerMinute)
            putExtra("minutesBefore", minutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(prayer, minutesBefore),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("TWKT", "Scheduled $prayer at $hour:$minute")
        } catch (e: SecurityException) {
            Log.e("TWKT", "Cannot schedule exact alarm for $prayer: ${e.message}")
        }
    }

    /**
     * true si l'appli peut programmer des alarmes exactes (Android 12+ / API 31+).
     * Si false, setExactAndAllowWhileIdle levera une SecurityException et
     * l'azan ne sonnera jamais en arriere-plan -> l'UI doit inviter l'utilisateur
     * a activer "Alarmes et rappels" via requestScheduleExactAlarmPermission().
     */
    @JavascriptInterface
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** Ouvre l'ecran systeme "Alarmes et rappels" pour cette appli (Android 12+). */
    @JavascriptInterface
    fun requestScheduleExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:" + context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("TWKT", "requestScheduleExactAlarmPermission failed: ${e.message}")
            }
        }
    }

    /**
     * Cancel all pending prayer notifications.
     */
    @JavascriptInterface
    fun cancelAllNotifications() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Meme requestCode que scheduleSinglePrayer (requestCodeFor) pour les
        // deux entrees possibles par priere : heure exacte (minutesBefore=0)
        // et alerte "N min avant" (minutesBefore=1 utilise ici juste comme
        // indicateur > 0, la valeur exacte n'importe pas pour le calcul du code).
        PRAYER_INDEX.keys.forEach { prayer ->
            listOf(0, 1).forEach { minutesBeforeFlag ->
                val intent = Intent(context, PrayerAlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context, requestCodeFor(prayer, minutesBeforeFlag), intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pi?.let { alarmManager.cancel(it) }
            }
        }
        Log.d("TWKT", "Cancelled all prayer notifications")
    }

    /**
     * Annule uniquement les alarmes d'alerte "N min avant" (garde intactes les
     * alarmes a l'heure exacte de l'azan). Appele depuis custom.js quand
     * l'utilisateur desactive ucAzanAlertEnabled : sans ca, une alerte deja
     * programmee avant la desactivation sonnerait quand meme une derniere fois.
     */
    @JavascriptInterface
    fun cancelAzanAlertAlarms() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        PRAYER_INDEX.keys.forEach { prayer ->
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, requestCodeFor(prayer, 1), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { alarmManager.cancel(it) }
        }
        Log.d("TWKT", "Cancelled azan alert (before-azan reminder) alarms")
    }

    /**
     * Programme la coupure puis la remise du son autour de l'azan de chaque
     * prière (onglet الإعدادات — custom.js, _ucScheduleSilentModeAlarms()).
     * Réutilise le même mécanisme AlarmManager.setExactAndAllowWhileIdle que
     * schedulePrayerNotifications() ci-dessus.
     *
     * @param jsonArray JSON: [{ prayer, muteHour, muteMinute, restoreHour, restoreMinute }]
     */
    @JavascriptInterface
    fun scheduleSilentModeAlarms(jsonArray: String) {
        try {
            cancelAllSilentModeAlarms()
            val items = JSONArray(jsonArray)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val prayer = obj.getString("prayer")
                scheduleSilentAction(
                    alarmManager, SilentModeReceiver.ACTION_MUTE, SilentModeReceiver.REASON_BEFORE,
                    obj.getInt("muteHour"), obj.getInt("muteMinute"), obj.optInt("muteDayOffset", 0),
                    prayer, 200 + i
                )
                scheduleSilentAction(
                    alarmManager, SilentModeReceiver.ACTION_RESTORE, SilentModeReceiver.REASON_BEFORE,
                    obj.getInt("restoreHour"), obj.getInt("restoreMinute"), obj.optInt("restoreDayOffset", 0),
                    prayer, 300 + i
                )
            }
            Log.d("TWKT", "Scheduled silent-mode alarms for ${items.length()} prayers")
        } catch (e: Exception) {
            Log.e("TWKT", "scheduleSilentModeAlarms error: ${e.message}")
        }
    }

    /**
     * Coupure courte et indépendante après l'azan (onglet الإعدادات —
     * custom.js, _ucScheduleQuickMuteAfterAzanAlarms()) : coupe pile à
     * l'heure de l'azan, remet N minutes après (0-30, défaut 5). Utilise le
     * même SilentModeReceiver que scheduleSilentModeAlarms() ci-dessus, mais
     * une reason ("after") et une plage de requestCode distinctes, pour que
     * les deux fonctionnalités puissent être actives ensemble sans que
     * l'une annule/écrase les alarmes de l'autre — leurs fenêtres de coupure
     * s'additionnent alors via le compteur par reason (cf. SilentModeReceiver).
     *
     * @param jsonArray JSON: [{ prayer, muteHour, muteMinute, restoreHour, restoreMinute }]
     */
    @JavascriptInterface
    fun scheduleQuickMuteAfterAzanAlarms(jsonArray: String) {
        try {
            cancelAllQuickMuteAfterAzanAlarms()
            val items = JSONArray(jsonArray)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val prayer = obj.getString("prayer")
                scheduleSilentAction(
                    alarmManager, SilentModeReceiver.ACTION_MUTE, SilentModeReceiver.REASON_AFTER,
                    obj.getInt("muteHour"), obj.getInt("muteMinute"), obj.optInt("muteDayOffset", 0),
                    prayer, 600 + i
                )
                scheduleSilentAction(
                    alarmManager, SilentModeReceiver.ACTION_RESTORE, SilentModeReceiver.REASON_AFTER,
                    obj.getInt("restoreHour"), obj.getInt("restoreMinute"), obj.optInt("restoreDayOffset", 0),
                    prayer, 700 + i
                )
            }
            Log.d("TWKT", "Scheduled quick-mute-after-azan alarms for ${items.length()} prayers")
        } catch (e: Exception) {
            Log.e("TWKT", "scheduleQuickMuteAfterAzanAlarms error: ${e.message}")
        }
    }

    private fun scheduleSilentAction(
        alarmManager: AlarmManager,
        action: String,
        reason: String,
        hour: Int,
        minute: Int,
        dayOffset: Int,
        prayer: String,
        requestCode: Int
    ) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            // ACTION_RESTORE décalée à la seconde 30 (ACTION_MUTE reste à 0) :
            // garantit que la remise du son est TOUJOURS programmée
            // strictement après la coupure, même quand les deux tombent sur
            // la même minute (ex. jauge "après l'azan" réglée sur 0) — sans
            // ce décalage, AlarmManager ne garantit pas l'ordre d'exécution
            // de deux alarmes au même instant, et une RESTORE traitée avant
            // sa MUTE laisserait le téléphone muet en permanence (plus aucune
            // alarme de remise programmée pour ce cycle).
            set(Calendar.SECOND, if (action == SilentModeReceiver.ACTION_RESTORE) 30 else 0)
            set(Calendar.MILLISECOND, 0)
            // dayOffset != 0 : la fenêtre calculée côté JS chevauche minuit
            // (ex. remise du son après Isha tardif en été) — cf. _minutesToHHMM
            // dans custom.js, qui renvoie un décalage de jour avec l'heure.
            if (dayOffset != 0) add(Calendar.DAY_OF_MONTH, dayOffset)
            // Meme correctif que scheduleSinglePrayer : si, meme apres dayOffset,
            // le moment calcule est deja passe (app rouverte tard), decaler d'un
            // jour supplementaire au lieu d'abandonner silencieusement la
            // programmation jusqu'a la prochaine reprogrammation.
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(context, SilentModeReceiver::class.java).apply {
            this.action = action
            putExtra("prayer", prayer)
            putExtra("reason", reason)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
            Log.d("TWKT", "Scheduled silent $action ($reason) for $prayer at $hour:$minute")
        } catch (e: SecurityException) {
            Log.e("TWKT", "Cannot schedule silent $action for $prayer: ${e.message}")
        }
    }

    /**
     * Annule toutes les alarmes de coupure/remise du son en attente (ne
     * touche pas à l'état actuel de la sonnerie ni au compteur de fenêtres —
     * cf. disableSilentMode() pour une restauration immédiate forcée).
     */
    @JavascriptInterface
    fun cancelAllSilentModeAlarms() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until 5) {
            cancelSilentPending(alarmManager, SilentModeReceiver.ACTION_MUTE, 200 + i)
            cancelSilentPending(alarmManager, SilentModeReceiver.ACTION_RESTORE, 300 + i)
        }
        Log.d("TWKT", "Cancelled all silent-mode alarms")
    }

    /**
     * Équivalent de cancelAllSilentModeAlarms() pour la coupure courte
     * après-azan (requestCodes 600-604/700-704, cf. scheduleQuickMuteAfterAzanAlarms).
     */
    @JavascriptInterface
    fun cancelAllQuickMuteAfterAzanAlarms() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until 5) {
            cancelSilentPending(alarmManager, SilentModeReceiver.ACTION_MUTE, 600 + i)
            cancelSilentPending(alarmManager, SilentModeReceiver.ACTION_RESTORE, 700 + i)
        }
        Log.d("TWKT", "Cancelled all quick-mute-after-azan alarms")
    }

    private fun cancelSilentPending(alarmManager: AlarmManager, action: String, requestCode: Int) {
        val intent = Intent(context, SilentModeReceiver::class.java).apply { this.action = action }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
    }

    /**
     * Désactive complètement le mode silencieux "avant l'azan" : annule les
     * alarmes en attente ET restaure tout de suite la sonnerie — sauf si la
     * coupure courte après-azan a elle-même encore une fenêtre ouverte, auquel
     * cas la sonnerie reste coupée jusqu'à SA propre remise (cf.
     * SilentModeReceiver.forceRestoreNow, compteur par reason). Appelé quand
     * l'utilisateur désactive ce bouton dans l'onglet الإعدادات (sinon le son
     * resterait coupé jusqu'à la prochaine RESTORE, qui vient d'être annulée).
     */
    @JavascriptInterface
    fun disableSilentMode() {
        cancelAllSilentModeAlarms()
        SilentModeReceiver.forceRestoreNow(context, SilentModeReceiver.REASON_BEFORE)
    }

    /**
     * Équivalent de disableSilentMode() pour la coupure courte après-azan —
     * ne restaure la sonnerie immédiatement que si "avant l'azan" n'a pas,
     * de son côté, une fenêtre encore ouverte (cf. reason-count partagé).
     */
    @JavascriptInterface
    fun disableQuickMuteAfterAzan() {
        cancelAllQuickMuteAfterAzanAlarms()
        SilentModeReceiver.forceRestoreNow(context, SilentModeReceiver.REASON_AFTER)
    }

    /** true si l'appli a actuellement coupé le son (au moins une fenêtre ouverte). */
    @JavascriptInterface
    fun isSilentModeActive(): Boolean = SilentModeReceiver.isAppSilencing(context)

    /**
     * Filet de sécurité : force la restauration de la sonnerie et remet à
     * zéro les compteurs des DEUX fonctionnalités (avant-azan et après-azan),
     * sans condition. Appelé depuis custom.js (_ucCheckStaleSilentMode) quand
     * le natif signale encore une coupure active (isSilentModeActive()) alors
     * qu'aucune fenêtre de coupure n'est censée couvrir l'heure actuelle
     * d'après les réglages/horaires de prière connus côté JS — situation qui
     * ne peut normalement se produire que si une alarme RESTORE programmée a
     * été manquée par l'OS (optimisation batterie agressive d'un
     * constructeur, redémarrage du téléphone avant reprogrammation, etc.).
     * Ne dépend pas de la programmation classique (AlarmManager) : agit
     * immédiatement, à l'ouverture/reprise de l'appli.
     */
    @JavascriptInterface
    fun forceRestoreAllSilentMode() {
        SilentModeReceiver.forceRestoreNow(context, SilentModeReceiver.REASON_BEFORE)
        SilentModeReceiver.forceRestoreNow(context, SilentModeReceiver.REASON_AFTER)
    }

    /** true si l'accès "Ne pas déranger" (requis pour changer le ringer mode) est accordé. */
    @JavascriptInterface
    fun hasDndAccess(): Boolean = SilentModeReceiver.hasNotificationPolicyAccess(context)

    /** Ouvre l'écran système de demande d'accès "Ne pas déranger". */
    @JavascriptInterface
    fun requestDndAccess() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TWKT", "requestDndAccess failed: ${e.message}")
        }
    }

    /**
     * Ouvre l'écran des réglages système de l'appli (fallback du bouton
     * "ma position" quand la permission de localisation a été refusée
     * définitivement par l'utilisateur — Android ne réaffiche alors plus
     * jamais le dialogue natif, seul un changement manuel dans les réglages
     * peut la réactiver).
     */
    @JavascriptInterface
    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TWKT", "openAppSettings failed: ${e.message}")
        }
    }

    /**
     * Pose le tag OneSignal mosque_id.
     * Appelé depuis custom.js au chargement de la page.
     * Permet à OneSignal de cibler les appareils par mosquée.
     *
     *   if (window.AndroidMobile) window.AndroidMobile.setMosqueId("tn.monastir.aboubakr");
     */
    @JavascriptInterface
    fun setMosqueId(mosqueId: String) {
        if (mosqueId.isNotBlank()) {
            OneSignal.User.addTag("mosque_id", mosqueId)
            Log.d("TWKT", "OneSignal tag mosque_id = $mosqueId")
        }
    }

    /**
     * Pose un tag OneSignal CUMULATIF par mosquée (mosque_sub_<id> = "1").
     * Appelé depuis custom.js pour CHAQUE mosquée de l'historique de l'utilisateur
     * (UC_MOSQUE_HISTORY), pas seulement la mosquée courante. Contrairement à
     * setMosqueId() (qui écrase mosque_id à chaque changement de mosquée), ce tag
     * n'est jamais retiré : il permet à l'utilisateur de continuer à recevoir les
     * notifications de toutes les mosquées déjà sélectionnées sur cet appareil,
     * même après être passé à une autre mosquée.
     *
     * L'id est sanitizé (caractères non alphanumériques → "_") car les tags
     * OneSignal sont utilisés comme clé ; ex: "tn.monastir.hidaya" → "tn_monastir_hidaya".
     * Cette sanitisation doit rester identique à celle utilisée côté
     * notify-mosque (Edge Function Supabase) pour que les filtres correspondent.
     *
     *   if (window.AndroidMobile) window.AndroidMobile.addMosqueSubscriptionTag("tn.monastir.hidaya");
     */
    @JavascriptInterface
    fun addMosqueSubscriptionTag(mosqueId: String) {
        if (mosqueId.isNotBlank()) {
            val sanitized = mosqueId.replace(Regex("[^a-zA-Z0-9_]"), "_")
            OneSignal.User.addTag("mosque_sub_$sanitized", "1")
            Log.d("TWKT", "OneSignal tag mosque_sub_$sanitized = 1")
        }
    }

    /**
     * Retire le tag OneSignal mosque_sub_<id> (sanitizé). Appelé depuis
     * custom.js quand l'utilisateur coupe les notifications d'une mosquée
     * de son historique (panneau "Mes notifications") — opt-out explicite,
     * symétrique de addMosqueSubscriptionTag().
     *
     *   if (window.AndroidMobile) window.AndroidMobile.removeMosqueSubscriptionTag("tn.monastir.hidaya");
     */
    @JavascriptInterface
    fun removeMosqueSubscriptionTag(mosqueId: String) {
        if (mosqueId.isNotBlank()) {
            val sanitized = mosqueId.replace(Regex("[^a-zA-Z0-9_]"), "_")
            OneSignal.User.removeTag("mosque_sub_$sanitized")
            Log.d("TWKT", "OneSignal tag mosque_sub_$sanitized retiré")
        }
    }

    /**
     * Check if running inside the Android app (callable from JS for feature detection).
     */
    @JavascriptInterface
    fun isAndroidApp(): Boolean = true

    /** Déclenché par le lien "Vérifier les mises à jour" du menu WebView. */
    @JavascriptInterface
    fun checkForAppUpdate() {
        onCheckForUpdate()
    }

    /**
     * Coupe la lecture native de l'azan (AzanPlaybackService / MediaPlayer),
     * meme mecanisme que le bouton "Arreter" de sa notification. Appele
     * depuis custom.js (_stopAzanAudio) quand l'utilisateur ferme la popup
     * azan dans l'appli, pour que les deux façons de couper le son
     * (notification ET popup) fonctionnent. Sans effet si rien ne joue :
     * AzanPlaybackService.onStartCommand traite ACTION_STOP silencieusement.
     *
     * prayer/source : transmis tels quels a NativeEventLog (NATIVE_STOP_USER)
     * pour que le journal distingue QUI a coupe (bouton notification vs popup
     * WebView) et QUELLE priere -- avant ce changement, cet appel ne
     * transmettait ni l'un ni l'autre, et NativeEventLog retombait sur le
     * prayer par defaut "Salat" (AzanPlaybackService.kt), rendant les deux
     * sources indiscernables dans le journal.
     */
    @JavascriptInterface
    fun stopAzanPlayback(prayer: String, source: String) {
        val intent = Intent(context, AzanPlaybackService::class.java).apply {
            action = AzanPlaybackService.ACTION_STOP
            putExtra("prayer", prayer)
            putExtra("source", source)
        }
        context.startService(intent)
    }

    /**
     * Active/desactive la coupure de l'azan par retournement du telephone
     * (onglet الإعدادات — custom.js, _ucToggleFlipToMuteAzan). Stocke dans les
     * SharedPreferences (pas dans le localStorage de la WebView) car c'est
     * AzanPlaybackService, un composant natif independant, qui doit lire ce
     * reglage pour decider d'activer ou non le capteur pendant la lecture.
     */
    @JavascriptInterface
    fun setFlipToMuteEnabled(enabled: Boolean) {
        context.getSharedPreferences(AzanPlaybackService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(AzanPlaybackService.PREF_FLIP_TO_MUTE, enabled)
            .apply()
        Log.d("TWKT", "Flip-to-mute azan: $enabled")
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return JSONObject().apply {
            put("model", Build.MODEL)
            put("sdk", Build.VERSION.SDK_INT)
            put("brand", Build.BRAND)
        }.toString()
    }

    /**
     * Reglage "demarrage automatique" (onglet الإعدادات — custom.js,
     * _ucToggleAutoStart). Meme schema que setFlipToMuteEnabled : stocke dans
     * les SharedPreferences (AutoStartPrefs), lues independamment par
     * BootReceiver sans jamais demarrer le WebView. Absent/sans effet sur un
     * boitier Android TV (cf. isAndroidTv ci-dessous, qui masque la ligne de
     * reglage correspondante cote JS).
     */
    @JavascriptInterface
    fun setAutoStartEnabled(enabled: Boolean) {
        AutoStartPrefs.setEnabled(context, enabled)
        Log.d("TWKT", "Auto-start on boot: $enabled")
    }

    @JavascriptInterface
    fun isAutoStartEnabled(): Boolean = AutoStartPrefs.isEnabled(context)

    @JavascriptInterface
    fun isAndroidTv(): Boolean = DeviceType.isAndroidTv(context)

    /**
     * Ouvre (au mieux) l'ecran constructeur de gestion du demarrage
     * automatique (MIUI/ColorOS/FuntouchOS/EMUI/...), avec repli sur l'ecran
     * standard "Infos sur l'application" — cf. AutoStartPermissionHelper.
     */
    @JavascriptInterface
    fun openAutoStartSettings() {
        AutoStartPermissionHelper.openAutoStartSettings(context)
    }

    @JavascriptInterface
    fun hasKnownAutoStartVendorSettings(): Boolean = AutoStartPermissionHelper.hasKnownVendorSettings()

    /** Etat courant du reglage "ecran d'accueil" (boitier TV uniquement). */
    @JavascriptInterface
    fun isTvHomeLauncherEnabled(): Boolean = TvHomeLauncherPrefs.isEnabled(context)

    /** Ouvre le menu outils TV (reglages Android type Wi-Fi / changer l'ecran
     *  d'accueil) — point d'entree pour un geste cache (ex. appui long sur
     *  le logo) puisqu'aucun ecran de reglages n'est accessible en mode
     *  horizontal. Seul moyen de rejoindre les reglages systeme une fois
     *  Tawkit defini comme ecran d'accueil (le bouton Accueil n'y mene
     *  plus). */
    @JavascriptInterface
    fun openTvUtilityMenu() {
        onOpenTvUtilityMenu()
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("TWKT_JS", message)
    }

    /**
     * Sauvegarde les données de prière pour le widget d'accueil.
     * Appelé depuis custom.js (_installWidgetBridge) après le calcul des
     * heures de prière. Déclenche aussitôt une mise à jour du widget.
     *
     * Format JSON attendu :
     *   {
     *     "mosque":    "جامع أبو بكر",
     *     "gregorian": "01/07/2026",
     *     "hijri":     "5 محرم 1448",
     *     "sunrise":   "05:47",
     *     "prayers": [
     *       { "name": "الفجر",  "time": "04:26", "minutes": 266 },
     *       { "name": "الظهر",  "time": "12:37", "minutes": 757 },
     *       { "name": "العصر",  "time": "16:22", "minutes": 982 },
     *       { "name": "المغرب", "time": "19:42", "minutes": 1182 },
     *       { "name": "العشاء", "time": "21:11", "minutes": 1271 }
     *     ],
     *     "savedAt": 1751379600000
     *   }
     */
    @JavascriptInterface
    fun savePrayerTimesForWidget(json: String) {
        try {
            context.getSharedPreferences(TawkitWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(TawkitWidgetProvider.KEY_PRAYER_DATA, json)
                .apply()
            TawkitWidgetProvider.triggerUpdate(context)
            Log.d("TWKT", "Widget prayer data saved & widget updated")
        } catch (e: Exception) {
            Log.e("TWKT", "savePrayerTimesForWidget error: ${e.message}")
        }
    }

    // ── Recitateurs Coran : stockage local (USB) + telechargement cloud ────
    // Toutes les methodes sont volontairement "pull" (pas de callback JS) :
    // le JS poll getImportStatus() / getDownloadProgress() via setInterval,
    // ce qui evite d'avoir a pousser des evenements depuis le natif.

    /** Liste des recitateurs deja installes localement. JSON: [{id,name,trackCount}] */
    @JavascriptInterface
    fun listReciters(): String = ReciterManager.listReciters(context)

    /** Ouvre le picker de dossier (SAF) pour importer depuis une cle USB / stockage externe. */
    @JavascriptInterface
    fun requestUsbImport() {
        Log.d("TWKT", "requestUsbImport() called from JS")
        onRequestImport()
    }

    /** Etat de l'import en cours. JSON: {status, message, importedCount} */
    @JavascriptInterface
    fun getImportStatus(): String = ReciterManager.getImportStatus()

    /** Supprime un recitateur installe localement (USB ou telecharge). */
    @JavascriptInterface
    fun removeReciter(id: String): Boolean = ReciterManager.removeReciter(context, id)

    /** URL file:// jouable directement par <audio>, ou "" si le fichier est absent. */
    @JavascriptInterface
    fun getReciterAudioUrl(id: String, surahNum: Int): String =
        ReciterManager.getAudioUrl(context, id, surahNum)

    /** Lance (ou relance) le telechargement resumable d'un recitateur (archive ZIP). */
    @JavascriptInterface
    fun startReciterDownload(id: String, url: String): String {
        return try {
            ReciterDownloadWorker.enqueue(context, ReciterManager.sanitizeId(id), url)
            JSONObject().apply { put("ok", true) }.toString()
        } catch (e: Exception) {
            JSONObject().apply { put("ok", false); put("error", e.message) }.toString()
        }
    }

    /** Met en pause (le fichier partiel est conserve pour reprise). */
    @JavascriptInterface
    fun pauseDownload(id: String) {
        ReciterDownloadWorker.pause(context, ReciterManager.sanitizeId(id))
    }

    /** Reprend un telechargement mis en pause (repart de l'octet ou il s'est arrete). */
    @JavascriptInterface
    fun resumeDownload(id: String, url: String) {
        ReciterDownloadWorker.enqueue(context, ReciterManager.sanitizeId(id), url)
    }

    /** Annule et supprime toute trace du telechargement (fichier partiel + progression). */
    @JavascriptInterface
    fun cancelDownload(id: String) {
        ReciterDownloadWorker.cancel(context, ReciterManager.sanitizeId(id))
    }

    /** Progression courante. JSON: {status, downloaded, total, percent, message?} */
    @JavascriptInterface
    fun getDownloadProgress(id: String): String =
        ReciterDownloadWorker.getProgress(context, ReciterManager.sanitizeId(id)).toString()

    // ── Catalogue azans (téléchargés) : stockage natif ─────────────────────
    // Remplace l'ancien cache IndexedDB (WebView) : fichiers dans
    // getExternalFilesDir(null)/azan_catalog/, scannés au lancement — voir
    // AzanCatalogManager. Même modèle "pull" (poll) que les récitateurs.

    /** Ids déjà installés localement. JSON: ["id1","id2",...] */
    @JavascriptInterface
    fun listInstalledAzanCatalog(): String = AzanCatalogManager.listInstalledIds(context)

    /** URL file:// jouable directement par <audio>, ou "" si absent localement. */
    @JavascriptInterface
    fun getAzanCatalogFileUrl(id: String): String = AzanCatalogManager.getFileUrl(context, id)

    /** Contenu JSON brut du catalogue bundlé (repli local fiable, cf. AzanCatalogManager). */
    @JavascriptInterface
    fun getLocalAzanCatalogJson(): String = AzanCatalogManager.readBundledCatalogJson(context)

    /** Lance le téléchargement d'un azan du catalogue (fichier audio unique). */
    @JavascriptInterface
    fun downloadAzanCatalogItem(id: String, url: String) {
        AzanCatalogManager.startDownload(context, id, url)
    }

    /** Etat du téléchargement en cours. JSON: {status, message?} — status: idle|downloading|done|error */
    @JavascriptInterface
    fun getAzanCatalogDownloadStatus(id: String): String = AzanCatalogManager.getDownloadStatus(id)

    // ── Riwayat Coran (textes) : stockage natif ────────────────────────────
    // Remplace l'ancien cache IndexedDB + le flag JS_CUSTOM.ucRiwayaInstalled
    // (localStorage) : fichiers dans getExternalFilesDir(null)/riwayat/,
    // scannés à l'ouverture du lecteur — voir RiwayaManager.

    /** Ids déjà installés localement. JSON: ["hafs","warsh",...] */
    @JavascriptInterface
    fun listInstalledRiwaya(): String = RiwayaManager.listInstalledIds(context)

    /**
     * Contenu texte (JSON) du fichier, ou "" si absent localement. Renvoyé en
     * String plutôt qu'une URL file:// : fetch(file://...) est rejeté par
     * Chromium/WebView côté client (aucune requête, "Failed to fetch" sans
     * trace console) — cf. RiwayaManager.readContent().
     */
    @JavascriptInterface
    fun getRiwayaContent(id: String): String = RiwayaManager.readContent(context, id)

    /** Lance le téléchargement d'un texte de riwaya. */
    @JavascriptInterface
    fun downloadRiwayaItem(id: String, url: String) {
        RiwayaManager.startDownload(context, id, url)
    }

    /** Etat du téléchargement en cours. JSON: {status, message?} — status: idle|downloading|done|error */
    @JavascriptInterface
    fun getRiwayaDownloadStatus(id: String): String = RiwayaManager.getDownloadStatus(id)

    /**
     * Empeche (ou autorise) l'ecran de s'eteindre / passer en veille pendant
     * que l'appli est au premier plan (FLAG_KEEP_SCREEN_ON sur la fenetre).
     * Appele depuis custom.js au debut/a la fin d'un telechargement de
     * recitateur : l'ecran qui s'eteint coupe le worker WorkManager en cours
     * (cf. logs RM/DL_STATUS "paused" avec message isStopped=true sans clic
     * pause prealable). Ne resout pas le cas "appli passee en arriere-plan"
     * (Android peut limiter le travail reseau meme ecran allume) — seulement
     * le cas ecran eteint/veille alors que l'appli reste au premier plan.
     *
     *   if (window.AndroidMobile) window.AndroidMobile.setKeepScreenOn(true);
     */
    @JavascriptInterface
    fun setKeepScreenOn(enabled: Boolean) {
        onSetKeepScreenOn(enabled)
    }
}
