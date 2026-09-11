package net.tawkit.mobile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * "Mode kiosque" boitier TV : filet de rattrapage arme par MainActivity.onStop()
 * quand l'appli sort du premier plan sans qu'on l'ait nous-memes decide (cf.
 * expectSystemHandoff). Boitier mural d'affichage continu des horaires de
 * priere -- contrairement a un telephone, il ne doit JAMAIS rester sur le
 * launcher constructeur, un ecran noir, ou une autre appli.
 *
 * Meme technique deja eprouvee que BootReceiver pour le lancement TV au
 * demarrage (AlarmManager.setExactAndAllowWhileIdle -- declenche par
 * AlarmManagerService, donc exempte des restrictions "Background Activity
 * Start" qui bloqueraient un simple startActivity() direct depuis
 * MainActivity.onStop(), cf. le commentaire detaille dans BootReceiver.kt),
 * mais relayee par CE BroadcastReceiver plutot qu'un PendingIntent.getActivity
 * direct : au moment ou l'alarme se declenche (quelques secondes plus tard),
 * on revrifie MainActivity.isAppInForeground -- l'utilisateur ou le systeme
 * peut tres bien etre revenu de lui-meme entretemps (cf. onResume() qui
 * annule ce rattrapage) -- plutot que de relancer Tawkit en aveugle.
 */
class AppForegroundWatchdogReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION = "net.tawkit.mobile.FOREGROUND_WATCHDOG"
        private const val REQUEST_CODE = 5501

        // Delai court : suffisant pour absorber une transition d'ecran normale
        // (anim de l'ancien launcher qui reprend la main un instant) sans
        // laisser Tawkit visible en arriere-plan plus que quelques secondes.
        private const val DELAY_MS = 3000L

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AppForegroundWatchdogReceiver::class.java).setAction(ACTION)
            return PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + DELAY_MS,
                pendingIntent(context)
            )
        }

        /** Appele par MainActivity.onResume() : un retour au premier plan --
         *  par nos soins ou par l'utilisateur -- rend ce rattrapage obsolete. */
        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (MainActivity.isAppInForeground) {
            Log.d("TWKT", "AppForegroundWatchdogReceiver — deja revenu au premier plan, rien a faire")
            return
        }
        Log.d("TWKT", "AppForegroundWatchdogReceiver — toujours en arriere-plan, relance de Tawkit")
        NativeEventLog.log(context, "SYS", "FOREGROUND_WATCHDOG — relance de Tawkit")
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(launch)
    }
}
