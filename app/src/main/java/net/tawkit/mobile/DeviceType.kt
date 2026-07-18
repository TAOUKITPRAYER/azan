package net.tawkit.mobile

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * Distingue un boîtier Android TV (écran mural, sans tactile) d'un téléphone
 * classique — utilisé par BootReceiver pour décider du comportement au
 * démarrage (plein écran sur TV, silencieux sur téléphone).
 *
 * Combine 3 signaux pour rester fiable même sur des boîtiers génériques/non
 * certifiés Google (ex. "BX TV") qui ne déclarent pas forcément
 * FEATURE_LEANBACK : le mode UI (positionné par le firmware, indépendant de
 * toute certification), FEATURE_LEANBACK, et l'absence de tactile.
 */
object DeviceType {
    fun isAndroidTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val pm = context.packageManager
        val uiModeIsTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val noTouchscreen = !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        return uiModeIsTv || hasLeanback || noTouchscreen
    }
}
