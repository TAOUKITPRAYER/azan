package net.tawkit.mobile

import android.app.AlarmManager
import android.app.PendingIntent
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
            if (TvHomeLauncherPrefs.isEnabled(context) && TvHomeLauncherHelper.isCurrentlyDefaultHome(context)) {
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
            // BUG (trouve 27/07/2026) : TvHomeLauncherPrefs.isEnabled() seul
            // reflete l'INTENTION (l'utilisateur a deja tente de definir
            // Tawkit comme accueil), pas la realite systeme. Sur les boitiers
            // ou l'ecran systeme de choix d'accueil crashe instantanement
            // (bug AOSP, cf. TvHomeLauncherPrefs), ce choix n'aboutit jamais :
            // isEnabled reste bloque a true alors que le vrai launcher par
            // defaut (resolu par le systeme) est celui du fabricant (ex.
            // com.vs.vslauncher). L'ancien code sautait alors purement et
            // simplement le lancement, en supposant a tort que l'OS s'en
            // chargeait -- l'appli ne demarrait donc jamais au boot. Verifier
            // isCurrentlyDefaultHome() en plus de isEnabled() avant de sauter
            // restaure le filet de secours exactement quand il est utile.
            // BUG (trouve 27/07/2026) : deux approches testees en conditions
            // reelles sur ce firmware (KM22) ont echoue :
            //  1) context.startActivity() direct, meme relaye via un foreground
            //     service avec startForeground() deja appele : catalogue
            //     "Background activity start" par ActivityTaskManager
            //     (isCallingUidForeground=false, isBgStartWhitelisted=false),
            //     processus tue juste apres, aucune UI, aucune exception.
            //  2) setFullScreenIntent() + PendingIntent.send() : le jeton BAL
            //     temporaire visible dans dumpsys notification
            //     ("whitelist: ...+30s0ms") ne suffit PAS a lui seul --
            //     isBgStartWhitelisted reste false et l'activite est bloquee de
            //     la meme facon (le declenchement plein-ecran automatique de
            //     setFullScreenIntent() n'existe de toute facon que si le
            //     keyguard est verrouille, absent sur ce boitier TV).
            // Fonctionne en revanche : programmer une alarme exacte
            // (AlarmManager.setExactAndAllowWhileIdle, meme mecanisme deja
            // utilise pour les alertes de priere, cf. MobileJsBridge) dont le
            // PendingIntent cible directement MainActivity. Quand
            // AlarmManagerService declenche ce PendingIntent (appel systeme,
            // pas depuis notre propre processus), Android accorde l'exemption
            // BAL correspondante -- comportement documente et deja exploite par
            // les applications de type reveil.
            Log.d("TWKT", "Boot completed (TV) — scheduling launch via AlarmManager")
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 2000,
                launchPendingIntent
            )
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
