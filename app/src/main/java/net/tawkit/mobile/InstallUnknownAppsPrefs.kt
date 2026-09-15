package net.tawkit.mobile

import android.content.Context

/**
 * Suivi "deja demande" pour l'autorisation "installer des applications
 * inconnues" (REQUEST_INSTALL_PACKAGES), boitiers Android TV uniquement --
 * cf. MainActivity.maybeRequestInstallUnknownAppsAccess(). Meme schema que
 * TvHomeLauncherPrefs/LockScreenPrefs : SharedPreferences natives, un seul
 * booleen "deja demande une fois", jamais reevalue depuis JS.
 */
object InstallUnknownAppsPrefs {
    private const val PREFS_NAME = "tawkit_install_unknown_apps_prefs"
    private const val KEY_SETUP_ASKED = "setup_asked"

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
