package net.tawkit.mobile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * Auto-reparation d'un gel du COMPOSITEUR WebView -- contexte GL perdu et
 * jamais retabli par le pilote GPU (Mali-G31 des boitiers TV bon marche en
 * tete), sans lien avec le thread JS qui, lui, continue de tourner.
 *
 * Incident declencheur (box tn.monastir.youssef, 08-09/09/2026, X96Q_Max_P) :
 * apres ~10 jours d'uptime en rendu materiel force (persist.tawkit.gpu_hw_ok=1),
 * le pilote a signale une perte de contexte (GL_UNKNOWN_CONTEXT_RESET_KHR) et
 * Chromium a boucle indefiniment a le recreer -- ecran fige 16 h sur une image
 * morte, gfxinfo "Total frames rendered: 0", un coeur CPU a 100 %, mais azan
 * natif et timers JS toujours actifs. Le watchdog anti-gel JS
 * (custom.js _installRepaintWatchdog) l'a bien detecte, mais sa seule arme --
 * location.reload() -- recharge le DOM dans le MEME process de rendu, donc le
 * MEME contexte GL mort : sans effet. Seul un kill du process nettoie l'etat.
 *
 * Deux reponses ici :
 *  1. requestRestart() : redemarre REELLEMENT le process (AlarmManager relance
 *     MainActivity, puis Process.killProcess) -- meme technique eprouvee que
 *     BootReceiver pour la relance au demarrage. Un gel de 16 h devient une
 *     coupure de ~10 s.
 *  2. Apprentissage : si le gel se repete (>= FALLBACK_THRESHOLD redemarrages
 *     dans FALLBACK_WINDOW_MS), on latch un flag persistant. Au prochain
 *     demarrage, DeviceType.isKnownBuggyGpu() le lit et force
 *     LAYER_TYPE_SOFTWARE -- la box se rabat toute seule sur le rendu logiciel
 *     stable, sans intervention adb (setprop persist.tawkit.gpu_force_sw 1).
 */
object GpuRecovery {

    private const val PREFS = "tawkit_gpu_recovery"
    private const val K_FORCE_SW    = "force_software_latched"   // Boolean, sticky une fois pose
    private const val K_RESTART_LOG = "restart_ts_csv"           // horodatages CSV (max 8, fenetre glissante)

    /** Un gel authentique ne se repare jamais plus vite qu'un cold boot. En
     *  deca, on suppose une autre cause (boucle) et on n'en refait pas. */
    private const val RESTART_COOLDOWN_MS = 90_000L

    /** Fenetre glissante pour compter les redemarrages "gel" recents. */
    private const val FALLBACK_WINDOW_MS = 45L * 60_000L

    /** Nb de redemarrages "gel" dans la fenetre au-dela duquel on latch le
     *  rendu logiciel pour de bon. 2 = "ca s'est reproduit -> ce GPU ne tient
     *  pas le rendu materiel sous charge reelle". */
    private const val FALLBACK_THRESHOLD = 2

    /** Plafond dur : une fois le rendu logiciel deja force, si ca continue de
     *  geler on arrete de tuer le process -- un gel en mode logiciel est un
     *  autre probleme (rafale reseau, cf. repaint-watchdog-net-shedding) que
     *  le reload JS gere, ou un hang noyau qu'aucun code applicatif ne corrige. */
    private const val MAX_RESTARTS_IN_WINDOW = 4

    private const val RELAUNCH_REQUEST_CODE = 4242

    enum class Result { RESTARTING, THROTTLED, GAVE_UP }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Lu par DeviceType.isKnownBuggyGpu(). Une fois a true, ne repasse jamais
     * a false tout seul (le seul moyen de re-tenter le materiel est un
     * setprop persist.tawkit.gpu_hw_ok 1 + clear manuel, ou une reinstall).
     */
    fun shouldForceSoftware(context: Context): Boolean =
        try { prefs(context).getBoolean(K_FORCE_SW, false) } catch (e: Exception) { false }

    /**
     * Demande de redemarrage suite a un gel du compositeur. Appelee depuis le
     * bridge (requestGpuRecoveryRestart, lui-meme appele par le watchdog JS
     * UNIQUEMENT apres qu'un location.reload() n'a pas degele l'ecran).
     *
     * NE REVIENT PAS quand elle redemarre (le process est tue). Renvoie un
     * Result seulement dans les cas throttled / give-up.
     */
    @Synchronized
    fun requestRestart(context: Context, reason: String): Result {
        val now = System.currentTimeMillis()
        val p = prefs(context)

        val history = (p.getString(K_RESTART_LOG, "") ?: "")
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { now - it in 0..FALLBACK_WINDOW_MS }

        val lastRestart = history.maxOrNull() ?: 0L
        if (lastRestart != 0L && now - lastRestart < RESTART_COOLDOWN_MS) {
            NativeEventLog.log(
                context, "SYS",
                "GPU_RECOVERY_THROTTLED reason=$reason sinceLastMs=${now - lastRestart}"
            )
            return Result.THROTTLED
        }

        val alreadySw = p.getBoolean(K_FORCE_SW, false)
        if (alreadySw && history.size >= MAX_RESTARTS_IN_WINDOW) {
            NativeEventLog.log(
                context, "SYS",
                "GPU_RECOVERY_GAVE_UP reason=$reason restartsInWindow=${history.size} forceSw=1"
            )
            return Result.GAVE_UP
        }

        val newHistory = (history + now).takeLast(8)
        val editor = p.edit().putString(K_RESTART_LOG, newHistory.joinToString(","))

        val latchNow = !alreadySw && newHistory.size >= FALLBACK_THRESHOLD
        if (latchNow) editor.putBoolean(K_FORCE_SW, true)
        editor.commit()

        if (latchNow) {
            NativeEventLog.log(
                context, "SYS",
                "GPU_AUTOFALLBACK_SW reason=$reason restartsInWindow=${newHistory.size} " +
                    "(rendu logiciel force au prochain demarrage)"
            )
        }
        NativeEventLog.log(
            context, "SYS",
            "GPU_RECOVERY_RESTART reason=$reason restartsInWindow=${newHistory.size} " +
                "forceSw=${latchNow || alreadySw}"
        )
        Log.w("TWKT", "GpuRecovery: hard-restarting process (reason=$reason)")

        scheduleRelaunch(context)

        // Laisse le commit() SharedPreferences + les writes NativeEventLog
        // (commit synchrone) finir de flusher avant de tuer le process.
        try { Thread.sleep(250) } catch (e: InterruptedException) { /* on tue quand meme */ }

        Process.killProcess(Process.myPid())
        exitProcess(10)   // filet si killProcess n'aboutit pas (renvoie Nothing)
    }

    /**
     * Programme la relance de MainActivity via AlarmManager : quand
     * AlarmManagerService (systeme) declenche le PendingIntent, Android
     * accorde l'exemption "background activity start" -- meme mecanisme que
     * BootReceiver (documente, exploite par les applis reveil). Une simple
     * startActivity() depuis notre process en train de mourir serait bloquee.
     */
    private fun scheduleRelaunch(context: Context) {
        try {
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pi = PendingIntent.getActivity(
                context, RELAUNCH_REQUEST_CODE, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val at = System.currentTimeMillis() + 1500L
            val canExact =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: Exception) {
            Log.e("TWKT", "GpuRecovery.scheduleRelaunch failed: ${e.message}")
        }
    }
}
