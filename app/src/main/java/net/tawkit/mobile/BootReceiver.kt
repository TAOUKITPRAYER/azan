package net.tawkit.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Après redémarrage : sur un boîtier Android TV (écran mural), relance
 * l'appli au premier plan comme avant — comportement volontairement
 * inchangé. Sur un téléphone, ne fait rien si l'utilisateur a désactivé le
 * démarrage automatique (AutoStartPrefs) ; sinon relance l'appli en mode
 * silencieux (EXTRA_SILENT_BOOT, cf. MainActivity) uniquement pour
 * recalculer les heures du jour et reprogrammer les alarmes natives — sans
 * jamais afficher d'UI ni interrompre l'écran d'accueil/verrouillage.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        // Boitiers Android TV generiques/non certifies : n'emettent souvent
        // pas ACTION_BOOT_COMPLETED mais l'une de ces actions "fast boot" a
        // la place (cf. AndroidManifest.xml pour le meme constat).
        private val ACCEPTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACCEPTED_ACTIONS) return
        Log.d("TWKT", "Boot receiver triggered by action=${intent.action}")

        if (DeviceType.isAndroidTv(context)) {
            if (TvHomeLauncherPrefs.isEnabled(context)) {
                // Le systeme lance deja Tawkit tout seul (ecran d'accueil
                // par defaut). Le relancer ici en plus creerait une seconde
                // instance concurrente du WebView : les deux tentent de lire
                // /ecrire le meme localStorage a quelques instants d'intervalle,
                // et l'une peut lire les donnees comme vides et les reinitialiser
                // -> perte apparente des reglages mosquee/heures deja
                // configures (constate sur X88 Pro 20). Rien a faire ici.
                Log.d("TWKT", "Boot completed (TV) — home launcher active, OS already launching, skipping")
                return
            }
            Log.d("TWKT", "Boot completed (TV) — relaunching in foreground")
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launch)
            return
        }

        if (!AutoStartPrefs.isEnabled(context)) {
            Log.d("TWKT", "Boot completed (phone) — autostart disabled by user, skipping")
            return
        }

        Log.d("TWKT", "Boot completed (phone) — relaunching silently for notification reschedule")
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_SILENT_BOOT, true)
        }
        context.startActivity(launch)
    }
}
