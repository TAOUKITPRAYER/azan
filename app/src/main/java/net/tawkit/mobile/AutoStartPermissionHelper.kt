package net.tawkit.mobile

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Ouvre, en meilleur effort, l'écran constructeur de gestion du "démarrage
 * automatique" (MIUI, ColorOS, FuntouchOS, EMUI, ...). Ces composants sont
 * non documentés par Google et peuvent changer/disparaître d'une version de
 * ROM à l'autre — c'est une limitation connue, aucune app tierce ne peut
 * garantir ce résultat par API officielle. Repli systématique sur l'écran
 * standard "Infos sur l'application" si le composant du constructeur est
 * introuvable ou que son lancement échoue.
 */
object AutoStartPermissionHelper {

    private data class VendorIntent(val pkg: String, val cls: String)

    // Une entrée par marque connue pour bloquer le démarrage auto par défaut.
    private val VENDOR_INTENTS = mapOf(
        "xiaomi" to VendorIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        "redmi" to VendorIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        "poco" to VendorIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        "huawei" to VendorIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        "honor" to VendorIntent("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        "oppo" to VendorIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        "realme" to VendorIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        "vivo" to VendorIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        "oneplus" to VendorIntent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
        "asus" to VendorIntent("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")
    )

    /** Utilisé côté JS (getDeviceInfo) pour n'afficher le bouton "réglages
     *  démarrage auto" que sur une marque connue pour restreindre l'autostart. */
    fun hasKnownVendorSettings(): Boolean = VENDOR_INTENTS.containsKey(Build.MANUFACTURER.lowercase())

    fun openAutoStartSettings(context: Context): Boolean {
        val vendor = VENDOR_INTENTS[Build.MANUFACTURER.lowercase()]
        if (vendor != null) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(vendor.pkg, vendor.cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.w("TWKT", "openAutoStartSettings: vendor screen unavailable (${e.message}) — falling back")
            }
        }
        return openAppDetailsSettings(context)
    }

    private fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.e("TWKT", "openAppDetailsSettings failed: ${e.message}")
            false
        }
    }
}
