package net.tawkit.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

/**
 * Monte le volume de la box au maximum juste avant chaque échéance sonore
 * (azan, récitation du Coran avant l'azan, takbir avant l'azan du Maghreb --
 * onglet "تعديل الأذان" — custom.js, _ucScheduleVolumeBoostAlarms()).
 * Contrairement à SilentModeReceiver (coupe puis remet), ce receiver ne fait
 * qu'AUGMENTER le volume, une seule fois par échéance — pas de restauration
 * après coup, conformément au réglage demandé ("systématiquement au
 * maximum").
 *
 * STREAM_MUSIC (lecture <audio> du WebView au premier plan) ET STREAM_ALARM
 * (AzanPlaybackService, USAGE_ALARM natif en arrière-plan, cf.
 * AzanPlaybackService.playAzan) sont tous les deux portés au maximum : selon
 * que l'appli est au premier plan ou non au moment de l'azan, c'est l'un ou
 * l'autre qui sert réellement à la lecture.
 */
class VolumeBoostReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: "Salat"
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            Log.d("TWKT", "VolumeBoostReceiver: volume au maximum avant $prayer")
            NativeEventLog.log(context, "AZAN", "VOLUME_BOOST_MAX prayer=$prayer")
        } catch (e: SecurityException) {
            Log.e("TWKT", "VolumeBoostReceiver: echec reglage volume - ${e.message}")
        }
    }
}
