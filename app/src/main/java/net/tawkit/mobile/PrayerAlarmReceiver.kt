package net.tawkit.mobile

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        /** Au-delà de ce retard par rapport à l'heure prévue, une alarme est
         *  considérée périmée (rattrapage suite à horloge figée) et ignorée
         *  plutôt que rejouée en retard. */
        private const val STALE_ALARM_THRESHOLD_MS = 10 * 60 * 1000L

        /** ID FIXE (pas prayer.hashCode()) : une nouvelle alerte "N min avant"
         *  (prière suivante) REMPLACE celle encore affichée pour la prière
         *  précédente au lieu de s'empiler dans le volet à côté d'elle. Sur
         *  toute une journée, sans ce fix, jusqu'à 5 notifications "azan_alert"
         *  (une par prière non balayée) restaient visibles en même temps —
         *  retour utilisateur 01/09/2026. Même principe côté SilentModeReceiver
         *  / HadithAlarmReceiver (id fixe par tag, cf. leurs commentaires).
         */
        private const val NOTIF_ID = 9101
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayer        = intent.getStringExtra("prayer") ?: "Salat"
        val prayerHour    = intent.getIntExtra("prayerHour", 0)
        val prayerMinute  = intent.getIntExtra("prayerMinute", 0)
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)
        val shortAzan     = intent.getBooleanExtra("shortAzan", false)
        val voiceMode     = intent.getBooleanExtra("voiceMode", true)
        val scheduledAtMillis = intent.getLongExtra("scheduledAtMillis", -1L)

        Log.d("TWKT", "Alerte $prayer dans $minutesBefore min — azan à $prayerHour:$prayerMinute")

        // Garde-fou anti-rafale : si l'horloge système est restée figée (perte
        // WiFi/NTP prolongée) puis se corrige d'un coup, AlarmManager déclenche
        // TOUTES les alarmes en attente à la suite en quelques secondes -- sans
        // ce contrôle, ça rejoue l'azan complet de plusieurs prières déjà
        // passées d'un coup (constaté : 5 azans lancés dans la même seconde,
        // mosquée Mediouni, 18/08/2026, après 15h d'horloge figée). On compare
        // l'heure réelle au moment du déclenchement à l'heure prévue
        // (scheduledAtMillis, posée par MobileJsBridge.scheduleSinglePrayer) :
        // au-delà de STALE_ALARM_THRESHOLD_MS de retard, on considère
        // l'alarme périmée et on l'ignore entièrement (pas de son, pas de
        // notification) plutôt que de rejouer un azan des heures en retard.
        if (scheduledAtMillis > 0) {
            val lagMs = System.currentTimeMillis() - scheduledAtMillis
            if (lagMs > STALE_ALARM_THRESHOLD_MS) {
                NativeEventLog.log(
                    context, "AZAN",
                    "ALARM_SKIP_STALE prayer=$prayer minutesBefore=$minutesBefore lagMin=" + (lagMs / 60000)
                )
                return
            }
        }

        if (minutesBefore == 0) {
            NativeEventLog.log(
                context, "AZAN",
                "ALARM_FIRE_AZAN prayer=$prayer time=" + String.format("%02d:%02d", prayerHour, prayerMinute) +
                    " appForeground=" + MainActivity.isAppInForeground
            )
        } else {
            NativeEventLog.log(context, "AZAN", "ALARM_FIRE_REMINDER prayer=$prayer minutesBefore=$minutesBefore")
        }

        MobileJsBridge.createNotificationChannel(context)

        // minutesBefore == 0 : c'est l'heure exacte de l'azan -> lecture native
        // du son reel (MediaPlayer, cf. AzanPlaybackService) au lieu de compter
        // sur le <audio> du WebView, qui ne tourne pas appli en arriere-plan.
        // Le service poste lui-meme SA notification (tap + bouton "Arreter") ;
        // on ne poste donc pas showNotification() dans ce cas pour eviter d'en
        // avoir deux pour le meme evenement.
        if (minutesBefore == 0) {
            val svcIntent = Intent(context, AzanPlaybackService::class.java).apply {
                putExtra("prayer", prayer)
                putExtra("prayerHour", prayerHour)
                putExtra("prayerMinute", prayerMinute)
                putExtra("shortAzan", shortAzan)
                putExtra("voiceMode", voiceMode)
            }
            ContextCompat.startForegroundService(context, svcIntent)
        } else {
            showNotification(context, prayer, prayerHour, prayerMinute, minutesBefore)
        }
    }

    /** Rappel "dans N min" avant l'azan (minutesBefore > 0 uniquement — la
     *  notification a l'heure exacte de l'azan est postee par
     *  AzanPlaybackService lui-meme, cf. onReceive ci-dessus). */
    private fun showNotification(
        context: Context,
        prayer: String,
        prayerHour: Int,
        prayerMinute: Int,
        minutesBefore: Int
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap sur la notification → ouvre l'app
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = "net.tawkit.mobile.OPEN_PRAYER"
            putExtra("prayer", prayer)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            context, prayer.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val azanTime = String.format("%02d:%02d", prayerHour, prayerMinute)

        val arabicName = mapOf(
            "Fajr"    to "الفجر",
            "Dhuhr"   to "الظهر",
            "Asr"     to "العصر",
            "Maghreb" to "المغرب",
            "Isha"    to "العشاء",
            "Jumua"   to "الجمعة"
        )[prayer] ?: prayer

        // Titre : "Fajr dans 5 min"
        val title = "$prayer dans $minutesBefore min — $arabicName"
        // Corps : heure de l'azan + invitation
        val body = "L'azan est à $azanTime\nحي على الصلاة — حي على الفلاح"

        val notification = NotificationCompat.Builder(context, MobileJsBridge.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)   // ouvre l'app au tap
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        // Le tag sépare cette alerte des notifications MUTE/RESTORE de la
        // même prière. ID fixe (pas prayer.hashCode()) : remplace l'alerte
        // encore affichée pour la prière précédente au lieu de s'y ajouter.
        nm.notify("azan_alert", NOTIF_ID, notification)
    }
}
