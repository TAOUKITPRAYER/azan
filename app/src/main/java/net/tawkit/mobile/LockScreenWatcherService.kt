package net.tawkit.mobile

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Service au premier plan, discret, qui surveille l'allumage de l'écran
 * (ACTION_SCREEN_ON ne peut être reçu que via un enregistrement dynamique
 * depuis un composant vivant -- exclu des broadcasts implicites statiques
 * depuis Android 3.1) pour afficher MainActivity par-dessus le verrouillage
 * (réglage "afficher Tawkit sur l'écran de verrouillage", téléphone
 * uniquement -- cf. LockScreenPrefs).
 *
 * Cible directement MainActivity (la VRAIE page principale, pas une copie) :
 * MainActivity.maybeActivateLockScreenGuard() y applique une couverture
 * transparente qui capte tous les touchers (même "clickable+focusable" que
 * splashOverlay, cf. activity_main.xml) sauf un glissement vers le haut --
 * demande explicite du 12/09/2026 (l'écran natif minimal précédent était
 * jugé "peu utile", l'utilisateur voulait une copie conforme de la page
 * principale, pas une vue dédiée reconstruite séparément).
 *
 * Démarré/arrêté depuis MobileJsBridge.setLockScreenModeEnabled() (bascule
 * en direct) et BootReceiver (persistance au redémarrage) -- jamais sur
 * boîtier TV (toujours au premier plan par conception, ce réglage n'a pas
 * de sens pour eux, cf. isAndroidTv aux deux points d'appel).
 *
 * Déclenchement via une notification à intention plein écran plutôt qu'un
 * startActivity() direct depuis le récepteur : depuis Android 10, le
 * démarrage d'une Activity depuis l'arrière-plan est fortement restreint --
 * la notification à intention plein écran (Notification.setFullScreenIntent)
 * est le mécanisme officiel prévu exactement pour ce cas d'usage (réveils,
 * appels entrants, cf. USE_FULL_SCREEN_INTENT dans le manifest), pas soumis
 * à cette restriction.
 *
 * ATTENTION (a ne PAS confondre avec le cas documente dans BootReceiver.kt,
 * ou setFullScreenIntent() avait echoue a contourner le blocage BAL sur un
 * boitier TV) : cet echec concernait un declenchement SANS keyguard
 * verrouille (BootReceiver relance l'appli au demarrage d'un boitier
 * toujours au premier plan, jamais verrouille) -- le declenchement
 * automatique plein-ecran de setFullScreenIntent() n'existe justement QUE
 * si le keyguard est verrouille au moment de la notification, precondition
 * verifiee explicitement ci-dessous (km.isKeyguardLocked) et absente de ce
 * cas TV. Notre scenario correspond exactement a la precondition documentee
 * par Android, contrairement a celui qui avait echoue.
 */
class LockScreenWatcherService : Service() {

    private var receiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        MobileJsBridge.createNotificationChannel(this)
        startForegroundCompat()
        registerScreenOnReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        receiver?.let { r -> runCatching { unregisterReceiver(r) } }
        receiver = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat() {
        val notif = NotificationCompat.Builder(this, MobileJsBridge.LOCK_SCREEN_WATCHER_CHANNEL_ID)
            .setContentTitle(getString(R.string.lock_screen_watcher_notif_title))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID_WATCHER, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID_WATCHER, notif)
        }
    }

    // ACTION_SCREEN_OFF n'est PLUS ecoute ici (retire le 12/09/2026) : ce
    // service, foreground mais priorite MIN, se fait regulierement tuer par
    // Samsung entre deux cycles (constate via dumpsys activity services :
    // restartTime != createTime) -- l'instantane "Tawkit etait-il au premier
    // plan" est desormais capture directement par MainActivity elle-meme
    // (cf. son propre recepteur SCREEN_OFF), qui ne peut par definition pas
    // etre mise en veille tant qu'elle est reellement au premier plan.
    private fun registerScreenOnReceiver() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                maybeShowLockScreen(ctx)
            }
        }
        receiver = r
        registerReceiver(r, filter)
    }

    private fun maybeShowLockScreen(context: Context) {
        try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            NativeEventLog.log(context, "SYS", "LOCK_SCREEN_SCREEN_ON isKeyguardLocked=${km.isKeyguardLocked}")
            if (!km.isKeyguardLocked) return

            val contentIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra(MainActivity.EXTRA_LOCK_SCREEN_LAUNCH, true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, REQUEST_CODE_FULLSCREEN, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(context, MobileJsBridge.LOCK_SCREEN_DISPLAY_CHANNEL_ID)
                .setContentTitle(getString(R.string.lock_screen_display_notif_title))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID_DISPLAY, notif)
            NativeEventLog.log(context, "SYS", "LOCK_SCREEN_FULLSCREEN_NOTIFIED")
            // Cette notification n'existe que pour porter setFullScreenIntent()
            // (obligatoire cote API Android, aucun autre moyen de declencher
            // l'affichage plein ecran depuis un service) -- elle n'a plus
            // aucune utilite une fois le lancement plein ecran declenche, et
            // ne doit pas trainer visible dans le volet (retour utilisateur
            // 12/09/2026 : "je ne veux plus de ces notifications"). Le
            // declenchement plein ecran est traite par NotificationManagerService
            // de facon synchrone a l'appel notify() ci-dessus -- l'annuler
            // juste apres ne l'empeche pas de s'etre deja produit.
            Handler(Looper.getMainLooper()).postDelayed({ nm.cancel(NOTIF_ID_DISPLAY) }, 500L)
        } catch (e: Exception) {
            Log.e("TWKT", "LockScreenWatcherService maybeShowLockScreen error: ${e.message}")
        }
    }

    companion object {
        private const val NOTIF_ID_WATCHER = 7701
        private const val NOTIF_ID_DISPLAY = 7702
        private const val REQUEST_CODE_FULLSCREEN = 7703

        fun start(context: Context) {
            val intent = Intent(context, LockScreenWatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenWatcherService::class.java))
        }
    }
}
