package net.tawkit.mobile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast

/**
 * Active/desactive l'activity-alias HOME (TvHomeLauncherAlias, cf.
 * AndroidManifest.xml) qui rend Tawkit selectionnable comme ecran d'accueil
 * Android, puis ouvre l'ecran systeme de choix du launcher par defaut pour
 * que l'utilisateur n'ait qu'a taper "Tawkit" une fois.
 *
 * L'etat active/desactive du composant est persistant au niveau du
 * PackageManager (survit aux redemarrages), independamment de
 * TvHomeLauncherPrefs qui ne sert qu'a piloter l'UI (case a cocher / dialogue
 * "deja demande").
 */
object TvHomeLauncherHelper {
    private val ALIAS_COMPONENT = ComponentName(
        "net.tawkit.mobile",
        "net.tawkit.mobile.TvHomeLauncherAlias"
    )

    fun setAliasEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            context.packageManager.setComponentEnabledSetting(
                ALIAS_COMPONENT, state, PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e("TWKT", "setAliasEnabled($enabled) failed: ${e.message}")
        }
    }

    /**
     * Vrai si Tawkit est actuellement resolu comme ecran d'accueil par
     * defaut par le systeme. Android ne permet a aucune appli tierce de se
     * redefinir seule et silencieusement comme accueil par defaut (choix
     * systeme, exige une confirmation utilisateur) : quand l'utilisateur
     * retourne au launcher d'origine puis re-choisit un accueil par defaut,
     * ce choix peut se perdre. Cette methode sert a le detecter pour
     * proposer une re-selection en un seul tap plutot que de forcer
     * l'utilisateur a se souvenir du geste d'appui long sur le logo.
     */
    fun isCurrentlyDefaultHome(context: Context): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(
            homeIntent, PackageManager.MATCH_DEFAULT_ONLY
        )
        if (resolved?.activityInfo?.packageName != context.packageName) return false
        // BUG (trouve 28/07/2026, boitier raven/Z6) : resolveActivity() peut
        // renvoyer notre package via une preference HOME persistee au niveau
        // systeme (mAlways=true, visible dans dumpsys package "Preferred
        // Activities") meme quand TvHomeLauncherAlias lui-meme est redevenu
        // disabled -- constate apres une reinstallation d'appli qui a
        // reinitialise le composant a son etat par defaut du manifest
        // (android:enabled="false") sans effacer cette preference systeme,
        // distincte et non liee. Sans ce controle, BootReceiver et
        // maybeReassertTvHomeLauncher() croient tous les deux a tort que le
        // systeme lancera Tawkit -- et aucun des deux ne fait quoi que ce
        // soit pour corriger la situation : l'appli ne demarre plus jamais,
        // ni au boot ni via la reassertion automatique.
        return context.packageManager.getComponentEnabledSetting(ALIAS_COMPONENT) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    /** Ouvre le selecteur systeme "application d'accueil" ; a defaut, guide
     *  l'utilisateur vers le bouton Accueil de la telecommande. */
    fun openHomeAppPicker(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e("TWKT", "ACTION_HOME_SETTINGS unavailable: ${e.message}")
            Toast.makeText(
                context,
                context.getString(R.string.tv_home_launcher_manual_hint),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
