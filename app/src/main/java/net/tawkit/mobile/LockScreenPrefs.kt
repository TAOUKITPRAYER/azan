package net.tawkit.mobile

import android.content.Context

/**
 * Réglage "afficher Tawkit sur l'écran de verrouillage" (téléphone uniquement,
 * cf. LockScreenActivity/LockScreenWatcherService — jamais utilisé sur
 * boîtier Android TV, toujours au premier plan par conception). Même schéma
 * que AutoStartPrefs : SharedPreferences natif, lu par BootReceiver sans
 * jamais démarrer la WebView.
 */
object LockScreenPrefs {
    const val PREFS_NAME = "tawkit_lockscreen_prefs"
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

    private const val KEY_WAS_FOREGROUND_AT_SCREEN_OFF = "was_foreground_at_screen_off"

    /**
     * Instantané de MainActivity.isAppInForeground pris par
     * LockScreenWatcherService exactement au moment ou l'ecran s'eteint
     * (ACTION_SCREEN_OFF) -- seul moyen fiable trouve pour savoir si Tawkit
     * etait reellement ce que l'utilisateur utilisait juste avant le
     * verrouillage. Les tentatives precedentes bases sur onStop()/onPause()
     * cote MainActivity se sont revelees peu fiables : ces callbacks peuvent
     * se declencher de facon transitoire pendant la choregraphie interne
     * d'Android/One UI pour afficher une Activity par-dessus un verrouillage
     * actif, independamment de l'etat reel avant verrouillage (constate en
     * conditions reelles, cf. NativeEventLog, 12/09/2026). En capturant au
     * moment precis de l'extinction de l'ecran -- avant toute intervention de
     * notre propre mecanisme de couverture -- on evite cette source de bruit.
     * Stocke en SharedPreferences (pas juste en memoire) : doit survivre a un
     * redemarrage du process entre l'extinction de l'ecran et son rallumage.
     */
    fun setWasForegroundAtScreenOff(context: Context, wasForeground: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WAS_FOREGROUND_AT_SCREEN_OFF, wasForeground)
            .apply()
    }

    fun wasForegroundAtScreenOff(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WAS_FOREGROUND_AT_SCREEN_OFF, false)
}
