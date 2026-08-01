package net.tawkit.mobile

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Composant minimal requis pour que l'app puisse devenir Device Owner
 * (cf. `dpm set-device-owner net.tawkit.mobile/.TawkitDeviceAdminReceiver`).
 * Aucune politique active (device_admin_receiver.xml est vide) -- seul le
 * statut de Device Owner nous intéresse, pour l'installation silencieuse de
 * mises à jour via PackageInstaller (cf. DeviceOwnerInstaller.kt), pas pour
 * restreindre l'appareil.
 */
class TawkitDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Log.d("TWKT", "TawkitDeviceAdminReceiver: enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.d("TWKT", "TawkitDeviceAdminReceiver: disabled")
    }
}
