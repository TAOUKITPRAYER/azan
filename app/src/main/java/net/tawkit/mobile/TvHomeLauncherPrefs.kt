package net.tawkit.mobile

import android.content.Context

/**
 * Reglage "Tawkit ecran d'accueil" (boitiers Android TV uniquement). Rendre
 * l'appli selectionnable comme launcher HOME est le moyen le plus fiable de
 * garantir un demarrage automatique : le systeme lance TOUJOURS l'appli
 * d'accueil par defaut au boot, independamment des restrictions de broadcast
 * BOOT_COMPLETED (delais, blocage vendeur, etc. — cf. BootReceiver, qui reste
 * comme filet de secours complementaire, pas remplace).
 *
 * Meme schema que AutoStartPrefs : SharedPreferences natives, lues sans
 * jamais demarrer le WebView.
 */
object TvHomeLauncherPrefs {
    const val PREFS_NAME = "tawkit_tv_home_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SETUP_ASKED = "setup_asked"
    private const val KEY_LAST_REASSERT_AT = "last_reassert_at"
    private const val KEY_REASSERT_FAIL_COUNT = "reassert_fail_count"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun hasAskedSetup(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SETUP_ASKED, false)

    fun markSetupAsked(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_ASKED, true)
            .apply()
    }

    /**
     * Anti-boucle pour maybeReassertTvHomeLauncher() (MainActivity) : sur
     * certains boitiers, l'ecran systeme "application d'accueil" (Settings.
     * ACTION_HOME_SETTINGS) crashe lui-meme instantanement (bug AOSP dans
     * com.android.packageinstaller.role.ui, NullPointerException sur
     * ActionBar absente) et rend la main a Tawkit, qui rappelle onResume()
     * -> reassert -> reouvre le picker -> re-crash, en boucle continue,
     * plusieurs fois par seconde. Constate en conditions reelles sur boitier
     * TV le 2026-07-26, l'appli paraissant "plantee" alors qu'aucune
     * exception ne se produit dans le process net.tawkit.mobile lui-meme.
     *
     * lastReassertAtMs impose un cooldown minimal entre deux tentatives ;
     * failCount compte les tentatives rapprochees (< cooldown l'une de
     * l'autre, signe que le picker crashe au lieu de laisser l'utilisateur
     * choisir) et desactive definitivement la reassertion automatique apres
     * MAX_FAILS, sans jamais toucher a KEY_ENABLED (l'utilisateur reste
     * libre de refaire le geste manuel via le dialogue / le logo).
     */
    const val REASSERT_COOLDOWN_MS = 10_000L
    const val REASSERT_MAX_FAILS = 3

    fun canAttemptReassert(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_REASSERT_FAIL_COUNT, 0) >= REASSERT_MAX_FAILS) return false
        val lastAt = prefs.getLong(KEY_LAST_REASSERT_AT, 0L)
        return System.currentTimeMillis() - lastAt >= REASSERT_COOLDOWN_MS
    }

    fun recordReassertAttempt(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val tooSoonToBeUserInitiated = now - prefs.getLong(KEY_LAST_REASSERT_AT, 0L) < REASSERT_COOLDOWN_MS
        val newFailCount = if (tooSoonToBeUserInitiated) {
            prefs.getInt(KEY_REASSERT_FAIL_COUNT, 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putLong(KEY_LAST_REASSERT_AT, now)
            .putInt(KEY_REASSERT_FAIL_COUNT, newFailCount)
            .apply()
    }

    /** Reinitialise le compteur d'echecs — appele quand la reassertion a
     *  reellement fonctionne (l'appli redevient l'accueil par defaut). */
    fun resetReassertFailCount(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REASSERT_FAIL_COUNT, 0)
            .apply()
    }
}
