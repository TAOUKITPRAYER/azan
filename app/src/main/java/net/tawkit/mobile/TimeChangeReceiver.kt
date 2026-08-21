package net.tawkit.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * Reagit a chaque correction de l'horloge systeme (ACTION_TIME_CHANGED --
 * diffuse par Android quand l'heure est modifiee, notamment par la
 * synchronisation NTP automatique juste apres l'obtention d'une connexion
 * internet).
 *
 * Confirme (20-21/08/2026, mosquee Mediouni) : hypothese initiale d'un lien
 * entre horloge fausse au demarrage (boitiers sans RTC a batterie fiable) et
 * alarmes AlarmManager livrees en rafale/ignorees. Chaine complete : au
 * demarrage a froid (coupure electrique), l'horloge systeme peut etre fausse
 * le temps que NTP la corrige -- si schedulePrayerNotifications() programme
 * la toute prochaine priere AVANT cette correction, l'alarme est armee avec
 * un scheduledAtMillis perime (cf. MobileJsBridge.scheduleSinglePrayer). Une
 * fois l'horloge corrigee, PrayerAlarmReceiver.STALE_ALARM_THRESHOLD_MS
 * (garde-fou anti-rafale ajoute le 18/08/2026 pour un incident different) la
 * compare a ce scheduledAtMillis perime et ignore silencieusement l'azan --
 * exactement le rapport utilisateur "l'azan de la prochaine priere suivant
 * une coupure electrique ne sonne pas". On reprogramme donc ici (via le meme
 * chemin JS que tout autre changement affectant les alarmes, cf.
 * MainActivity.rescheduleNativeAzanAlarmsIfReady -> custom.js
 * _ucRescheduleNativeAzanAlarms) des que l'horloge bouge, avec un
 * scheduledAtMillis desormais a jour.
 */
class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED) return

        val newTime = System.currentTimeMillis()
        val uptimeMs = SystemClock.elapsedRealtime()
        val msg = "SYSTEM_TIME_CHANGED newTime=$newTime uptimeMs=$uptimeMs"
        Log.d("TWKT", msg)
        NativeEventLog.log(context, "SYS", msg)

        MainActivity.rescheduleNativeAzanAlarmsIfReady()
    }
}
