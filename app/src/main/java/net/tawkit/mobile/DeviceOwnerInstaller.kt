package net.tawkit.mobile

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Installation silencieuse via l'API officielle PackageInstaller -- disponible
 * SEULEMENT si l'app a le statut Device Owner (cf. TawkitDeviceAdminReceiver +
 * "dpm set-device-owner", qui exige qu'aucun compte ne soit présent sur
 * l'appareil au moment de la commande, une seule fois par box). Contrairement
 * à SilentUpdateHelper (su, quasi toujours refusé -- cf. son commentaire de
 * classe), c'est un mécanisme officiel documenté par Google et utilisé par les
 * solutions MDM professionnelles (Hexnode, Knox...) : quand l'appelant est le
 * Device Owner, PackageInstaller.Session.commit() n'affiche AUCUNE boîte de
 * confirmation, comportement standard depuis Android 5.0 (API 21).
 *
 * Vérifié en conditions réelles le 31/07/2026 (X88 Pro 20) : après retrait du
 * compte Google et "dpm set-device-owner", isDeviceOwner() devient vrai.
 */
object DeviceOwnerInstaller {

    private const val ACTION_INSTALL_RESULT = "net.tawkit.mobile.DEVICE_OWNER_INSTALL_RESULT"

    // Filet de sécurité (ajouté 14/09/2026) : sans lui, la suspendCancellableCoroutine
    // ci-dessous attend indéfiniment le broadcast ACTION_INSTALL_RESULT -- si ce
    // broadcast n'arrive jamais pour une raison quelconque (bug firmware, session
    // PackageInstaller qui n'aboutit jamais, receiver perdu après un
    // MY_PACKAGE_REPLACED partiel...), le coroutine reste suspendu POUR TOUJOURS,
    // ce qui bloque aussi RemoteSilentUpdater.run() en amont (jamais de repli su/
    // installateur système, jamais de rapport d'échec). Découvert en creusant un
    // autre incident (box tn.raoued.nour-chaker, 14/09/2026, cf. commentaire de
    // MainActivity.maybeRequestInstallUnknownAppsAccess) où CE chemin-ci n'était
    // pas en cause (box non Device Owner) mais le risque de blocage permanent est
    // le même pour toute box qui l'est. ~96 Mo : dexopt/vérification peuvent
    // légitimement prendre du temps, d'où une marge large plutôt qu'un timeout
    // serré comme SilentUpdateHelper (pm install en ligne de commande, plus rapide).
    private const val INSTALL_TIMEOUT_MS = 90_000L

    fun isDeviceOwner(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Crée une session PackageInstaller, y écrit l'APK, puis commit() --
     * suspend jusqu'à ce que le broadcast de résultat (status SUCCESS/FAILURE)
     * revienne, via un BroadcastReceiver enregistré dynamiquement (jamais dans
     * le manifest : ne doit exister que le temps de CETTE installation).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun installSilently(context: Context, apkFile: File): Boolean {
        if (!isDeviceOwner(context)) return false
        if (!apkFile.exists()) return false

        // withTimeoutOrNull annule le coroutine interne si INSTALL_TIMEOUT_MS
        // s'écoule sans réponse -- cont.invokeOnCancellation (ci-dessous) se
        // charge de désenregistrer le receiver proprement dans ce cas, comme
        // pour toute autre annulation.
        return withTimeoutOrNull(INSTALL_TIMEOUT_MS) { installSilentlyInternal(context, apkFile) } ?: false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun installSilentlyInternal(context: Context, apkFile: File): Boolean {
        return suspendCancellableCoroutine { cont ->
            var receiver: BroadcastReceiver? = null
            try {
                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                params.setSize(apkFile.length())
                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                session.use { s ->
                    FileInputStream(apkFile).use { input ->
                        s.openWrite("tawkit_update", 0, apkFile.length()).use { out ->
                            input.copyTo(out)
                            s.fsync(out)
                        }
                    }

                    val localReceiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            Log.d("TWKT", "DeviceOwnerInstaller: install result status=$status message=$message")
                            try { context.unregisterReceiver(this) } catch (e: Exception) { /* déjà désenregistré */ }
                            if (cont.isActive) cont.resume(status == PackageInstaller.STATUS_SUCCESS, null)
                        }
                    }
                    receiver = localReceiver
                    val filter = IntentFilter(ACTION_INSTALL_RESULT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(localReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        @Suppress("UnspecifiedRegisterReceiverFlag")
                        context.registerReceiver(localReceiver, filter)
                    }

                    val resultIntent = Intent(ACTION_INSTALL_RESULT).setPackage(context.packageName)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, sessionId, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                    s.commit(pendingIntent.intentSender)
                }

                cont.invokeOnCancellation {
                    receiver?.let { try { context.unregisterReceiver(it) } catch (e: Exception) {} }
                }
            } catch (e: Exception) {
                Log.d("TWKT", "DeviceOwnerInstaller: EXCEPTION ${e.javaClass.simpleName}: ${e.message}")
                receiver?.let { try { context.unregisterReceiver(it) } catch (ex: Exception) {} }
                if (cont.isActive) cont.resume(false, null)
            }
        }
    }
}
