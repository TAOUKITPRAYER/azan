package net.tawkit.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * Journalise (sans rien reprogrammer) chaque correction de l'horloge systeme
 * (ACTION_TIME_CHANGED -- diffuse par Android quand l'heure est modifiee,
 * notamment par la synchronisation NTP automatique juste apres l'obtention
 * d'une connexion internet). Purement diagnostique pour l'instant : sert a
 * confirmer ou infirmer l'hypothese d'un lien entre horloge fausse au
 * demarrage (boitiers sans RTC a batterie fiable, ex. KM22) et alarmes
 * AlarmManager livrees en rafale au reveil de la synchro (cf. rapport debug
 * 11/08/2026, plusieurs azans simultanes -- corrige cote AzanPlaybackService,
 * mais la cause du timing des alarmes elle-meme reste a confirmer avant de
 * toucher a la logique de programmation). Si ce log confirme l'hypothese,
 * l'etape suivante serait de reprogrammer les alarmes prieres ici.
 */
class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED) return

        val newTime = System.currentTimeMillis()
        val uptimeMs = SystemClock.elapsedRealtime()
        val msg = "SYSTEM_TIME_CHANGED newTime=$newTime uptimeMs=$uptimeMs"
        Log.d("TWKT", msg)
        NativeEventLog.log(context, "SYS", msg)
    }
}
