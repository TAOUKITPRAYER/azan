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
 * Cible : STREAM_MUSIC UNIQUEMENT. C'est le flux de la lecture <audio> du
 * WebView (Coran, takbir) ET, sur un boîtier de mosquée (appli toujours au
 * premier plan), de l'azan lui-même -- AzanPlaybackService route désormais
 * l'azan sur STREAM_MUSIC/USAGE_MEDIA quand l'appli est au premier plan, pour
 * qu'il suive le volume réglé à la télécommande. STREAM_ALARM n'est plus
 * touché : le forcer masquait le réglage manuel du responsable, et l'azan
 * en arrière-plan (téléphone) doit rester au volume d'alarme choisi par
 * l'utilisateur.
 *
 * EXCEPTION -- coupure manuelle explicite : si le volume média (STREAM_MUSIC)
 * est à 0 au moment où le boost devrait s'appliquer, on considère que
 * l'utilisateur a délibérément coupé le son de la box (maintenance, nuit,
 * circonstance particulière...) et on NE force RIEN. Sans ce garde-fou,
 * mettre le volume à 0 ne "tenait" pas : 2 min avant l'azan suivant, le boost
 * le rétablissait au maximum et l'azan sortait à fond sur les baffles
 * (constaté mosquée tn.monastir.aboubakr). Dès que le volume média repasse
 * au-dessus de 0, le boost redevient actif.
 */
class VolumeBoostReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: "Salat"
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            // Coupure manuelle explicite (volume média à 0) -> on respecte, on
            // ne remonte rien. Voir le commentaire de classe ci-dessus.
            if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                Log.d("TWKT", "VolumeBoostReceiver: STREAM_MUSIC=0 (coupure manuelle) -> boost ignore avant $prayer")
                NativeEventLog.log(context, "AZAN", "VOLUME_BOOST_SKIP_MUTED prayer=$prayer")
                return
            }
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            Log.d("TWKT", "VolumeBoostReceiver: STREAM_MUSIC au maximum avant $prayer")
            NativeEventLog.log(context, "AZAN", "VOLUME_BOOST_MAX prayer=$prayer")
        } catch (e: SecurityException) {
            Log.e("TWKT", "VolumeBoostReceiver: echec reglage volume - ${e.message}")
        }
    }
}
