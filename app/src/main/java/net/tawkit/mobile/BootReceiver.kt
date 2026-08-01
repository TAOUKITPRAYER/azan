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
        // MY_PACKAGE_REPLACED (31/07/2026) : meme traitement qu'un boot --
        // necessaire pour la mise a jour silencieuse a distance
        // (RemoteSilentUpdater/SilentUpdateHelper) qui remplace l'APK sans
        // jamais passer par l'ecran d'installation systeme. Sur les box ou
        // Tawkit est deja alias HOME actif, l'OS relance seul l'activite HOME
        // apres remplacement (constate en conditions reelles) -- mais ce
        // n'est pas garanti sur toutes les box (alias HOME desactive par
        // defaut, cf. TvHomeLauncherPrefs), d'ou ce filet explicite qui
        // reutilise la meme technique AlarmManager deja eprouvee ci-dessous
        // plutot que de supposer que l'auto-relance HOME suffira partout.
        private val ACCEPTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACCEPTED_ACTIONS) return
        Log.d("TWKT", "Boot receiver triggered by action=${intent.action}")

        if (DeviceType.isAndroidTv(context)) {
            if (MainActivity.isAppInForeground) {
                // Une instance de MainActivity tourne deja reellement dans ce
                // process (le systeme a deja lance Tawkit comme ecran
                // d'accueil ; un BroadcastReceiver sans android:process dedie
                // s'execute dans le process existant de l'appli s'il tourne
                // deja). C'est le seul signal fiable a 100% : le relancer ici
                // en plus creerait une seconde instance concurrente du WebView,
                // les deux tentant de lire/ecrire le meme localStorage a
                // quelques instants d'intervalle, l'une pouvant lire les
                // donnees comme vides et les reinitialiser -> perte apparente
                // des reglages mosquee/heures deja configures (constate sur
                // X88 Pro 20). Rien a faire ici.
                Log.d("TWKT", "Boot completed (TV) — MainActivity already running, skipping")
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
            // chargeait -- l'appli ne demarrait donc jamais au boot.
            // BUG (trouve 28/07/2026, boitier raven/Z6) : meme en verifiant
            // EN PLUS TvHomeLauncherHelper.isCurrentlyDefaultHome() (qui
            // interroge PackageManager.resolveActivity), le signal reste
            // trompeur -- confirme en conditions reelles : sur ce boitier,
            // PackageManager rapporte Tawkit comme alias HOME correctement
            // resolu ET actif (COMPONENT_ENABLED_STATE_ENABLED), et pourtant
            // le systeme ne le lance jamais reellement au boot (firmware
            // generique/non certifie, cf. commentaire DeviceType). Deux
            // signaux PackageManager de suite se sont donc reveles non
            // fiables sur des boitiers differents. isAppInForeground
            // court-circuite definitivement ce probleme : au lieu de deviner
            // ce que le systeme va faire, on verifie l'unique fait qui
            // compte reellement -- Tawkit tourne-t-il deja au premier plan,
            // oui ou non. Le filet de secours AlarmManager ci-dessous
            // s'execute donc desormais dans tous les autres cas.
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
