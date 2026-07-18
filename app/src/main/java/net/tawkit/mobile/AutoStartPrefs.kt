package net.tawkit.mobile

import android.content.Context

/**
 * Réglage "lancement automatique au démarrage" pour téléphones (les
 * boîtiers Android TV gardent leur comportement toujours-actif existant,
 * cf. BootReceiver — ce réglage ne s'applique jamais à eux). Stocké dans des
 * SharedPreferences natives (pas le localStorage de la WebView) car
 * BootReceiver doit pouvoir le lire sans jamais démarrer le WebView — même
 * schéma que AzanPlaybackService.PREFS_NAME/PREF_FLIP_TO_MUTE.
 */
object AutoStartPrefs {
    const val PREFS_NAME = "tawkit_autostart_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SETUP_ASKED = "setup_asked"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

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
