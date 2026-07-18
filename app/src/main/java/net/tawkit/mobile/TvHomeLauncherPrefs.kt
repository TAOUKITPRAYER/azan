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
}
