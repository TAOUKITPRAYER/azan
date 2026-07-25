package net.tawkit.mobile

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * Notification native "hadith avant la priere" (10 min avant chaque azan,
 * reglage independant JS_CUSTOM.ucHadithReminderEnabled, cf. custom.js
 * _installHadithReminder). Recepteur DEDIE (pas PrayerAlarmReceiver) : son
 * propre composant Kotlin garantit qu'aucune de ses alarmes ne peut jamais
 * entrer en collision/ecraser celles de PrayerAlarmReceiver, meme si un
 * requestCode entier venait a coincider -- MobileJsBridge.requestCodeFor()
 * (utilise pour l'azan/le rappel "N min avant") ne distingue que
 * minutesBefore>0 vs ==0, pas sa valeur exacte : un rappel hadith fixe a
 * 10 min pourrait sinon silencieusement ecraser le rappel "N min avant azan"
 * existant si l'utilisateur a regle N=10 de son cote. L'identite d'un
 * PendingIntent incluant son composant cible, deux classes different suffit
 * a les rendre distincts sans toucher au schema de request code existant.
 *
 * Contenu de la notification (titre + corps) toujours en ARABE, quelle que
 * soit la langue de l'interface -- choix explicite (discussion 25/07/2026) :
 * le texte du hadith est un contenu religieux authentique, jamais traduit
 * pour eviter tout risque de nuance perdue ; le reste de la notification
 * reste donc dans la meme langue pour ne pas la fragmenter.
 */
class HadithAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayer        = intent.getStringExtra("prayer") ?: "Salat"
        val arabicName    = intent.getStringExtra("arabicName") ?: prayer
        val hadithText     = intent.getStringExtra("hadithText") ?: return
        val hadithSource   = intent.getStringExtra("hadithSource") ?: ""

        NativeEventLog.log(context, "HADITH", "ALARM_FIRE prayer=$prayer")

        MobileJsBridge.createNotificationChannel(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = "net.tawkit.mobile.OPEN_PRAYER"
            putExtra("prayer", prayer)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            context, 900 + prayer.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "$arabicName بعد 10 دقائق"
        val body  = if (hadithSource.isNotEmpty()) "$hadithText\n\n$hadithSource" else hadithText

        val notification = NotificationCompat.Builder(context, MobileJsBridge.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(hadithText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .build()

        // Tag dedie : coexiste sans jamais remplacer azan_alert (rappel "N min
        // avant") ni les notifications AzanPlaybackService pour la meme priere.
        nm.notify("hadith_reminder", prayer.hashCode(), notification)
    }
}
