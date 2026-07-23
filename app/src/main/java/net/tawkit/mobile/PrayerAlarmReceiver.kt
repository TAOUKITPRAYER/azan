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

    override fun onReceive(context: Context, intent: Intent) {
        val prayer        = intent.getStringExtra("prayer") ?: "Salat"
        val prayerHour    = intent.getIntExtra("prayerHour", 0)
        val prayerMinute  = intent.getIntExtra("prayerMinute", 0)
        val minutesBefore = intent.getIntExtra("minutesBefore", 0)
        val shortAzan     = intent.getBooleanExtra("shortAzan", false)
        val voiceMode     = intent.getBooleanExtra("voiceMode", true)

        Log.d("TWKT", "Alerte $prayer dans $minutesBefore min — azan à $prayerHour:$prayerMinute")

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
        // même prière. Elle reste dans le volet jusqu'au clic ou au balayage.
        nm.notify("azan_alert", prayer.hashCode(), notification)
    }
}
