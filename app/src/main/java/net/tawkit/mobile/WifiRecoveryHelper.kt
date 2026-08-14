package net.tawkit.mobile

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import kotlin.concurrent.thread
import org.json.JSONObject

/**
 * Detecte une horloge systeme incoherente (boitiers TV sans RTC a batterie
 * fiable, cf. TimeChangeReceiver) et tente une reprise reseau pour laisser
 * Android resynchroniser l'heure via NTP -- cf. plan "Detection horloge
 * systeme incoherente + reprise Wi-Fi" (11/08/2026).
 *
 * isSystemClockPlausible() compare l'heure systeme a lastUpdateTime de
 * l'appli elle-meme (PackageManager) : aucune date codee en dur a maintenir,
 * se remet a jour tout seul a chaque mise a jour -- l'appli ne peut, par
 * definition, jamais tourner AVANT sa propre derniere installation/MAJ.
 *
 * startRecovery() ne cycle les reseaux Wi-Fi DEJA ENREGISTRES que si l'appli
 * est Device Owner (cf. DeviceOwnerInstaller.isDeviceOwner) : l'API
 * WifiManager.enableNetwork()/getConfiguredNetworks() est depreciee depuis
 * Android 10 et non fonctionnelle pour une app tierce ordinaire ciblant
 * l'API 29+. Sans ce statut, on se contente de detecter l'etat de connexion
 * (status=unsupported si non connecte) -- custom.js relaie alors vers
 * l'utilisateur (bouton "Ouvrir les reglages Wi-Fi", cf. openWifiSettings).
 */
object WifiRecoveryHelper {

    const val STATUS_IDLE        = "idle"
    const val STATUS_CHECKING    = "checking"
    const val STATUS_TRYING      = "trying"
    const val STATUS_CONNECTED   = "connected"
    const val STATUS_UNSUPPORTED = "unsupported"
    const val STATUS_FAILED      = "failed"

    private const val PER_NETWORK_TIMEOUT_MS = 8000L
    private const val POLL_STEP_MS           = 300L

    @Volatile
    private var status: String = STATUS_IDLE

    @Volatile
    private var recoveryRunning = false

    /** SSID (sans guillemets) en cours de tentative -- relu par custom.js
     *  pour afficher une progression "essai en cours : XXX" reseau par
     *  reseau. Vide si aucune tentative Wi-Fi en cours (deja connecte,
     *  unsupported, ou pas encore demarre). */
    @Volatile
    private var currentSsid: String = ""

    /** SSID reellement connecte a la fin (status=connected via cycle
     *  Wi-Fi) -- distinct du cas "deja connecte" (Ethernet ou Wi-Fi deja
     *  actif avant meme startRecovery), ou cette valeur reste vide : dans
     *  ce cas custom.js n'a pas de nom de reseau specifique a afficher. */
    @Volatile
    private var connectedSsid: String = ""

    private fun stripQuotes(ssid: String?): String {
        if (ssid == null) return ""
        return if (ssid.length >= 2 && ssid.startsWith("\"") && ssid.endsWith("\""))
            ssid.substring(1, ssid.length - 1) else ssid
    }

    fun isSystemClockPlausible(context: Context): Boolean {
        return try {
            val lastUpdateTime = context.packageManager
                .getPackageInfo(context.packageName, 0).lastUpdateTime
            System.currentTimeMillis() >= lastUpdateTime
        } catch (e: Exception) {
            Log.e("TWKT", "WifiRecoveryHelper: isSystemClockPlausible error: ${e.message}")
            true   // en cas de doute, ne pas bloquer l'appli
        }
    }

    /**
     * true si une connexion reseau VALIDEE existe deja, Wi-Fi OU Ethernet --
     * le but reel est "cette box peut-elle synchroniser l'heure via NTP",
     * pas specifiquement "est-elle en Wi-Fi". Beaucoup de boitiers sont
     * cables en Ethernet (constate en test, box Droidlogic S905W2,
     * 11/08/2026) : s'arreter au seul transport Wi-Fi les faisait passer a
     * tort par "non connecte" -> tentative de reprise Wi-Fi inutile alors
     * que la box a deja internet.
     */
    fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e("TWKT", "WifiRecoveryHelper: isNetworkConnected error: ${e.message}")
            false
        }
    }

    fun getStatus(): String = status
    fun getCurrentSsid(): String = currentSsid
    fun getConnectedSsid(): String = connectedSsid

    /**
     * Force la synchronisation automatique date/heure (Settings.Global.
     * AUTO_TIME/AUTO_TIME_ZONE) via DevicePolicyManager.setGlobalSetting() --
     * API reservee au Device Owner (WRITE_SECURE_SETTINGS n'est pas
     * accordable a une app tierce ordinaire). Sans ca, une box dont ce
     * reglage a ete desactive (usine, firmware, ou desactive par erreur)
     * pourrait rester connectee au reseau SANS jamais resynchroniser
     * l'horloge -- ce forcage est ce qui fait reellement fonctionner la
     * reprise reseau, pas seulement la connexion elle-meme.
     */
    private fun forceAutoTimeSync(context: Context) {
        if (!DeviceOwnerInstaller.isDeviceOwner(context)) return
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(context, TawkitDeviceAdminReceiver::class.java)
            dpm.setGlobalSetting(admin, Settings.Global.AUTO_TIME, "1")
            dpm.setGlobalSetting(admin, Settings.Global.AUTO_TIME_ZONE, "1")
            NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_FORCED_AUTO_TIME")
        } catch (e: Exception) {
            Log.e("TWKT", "WifiRecoveryHelper: forceAutoTimeSync error: ${e.message}")
        }
    }

    /** Fire-and-forget : lance la reprise sur un thread dedie, statut relu via getStatus(). */
    fun startRecovery(context: Context) {
        if (recoveryRunning) return
        recoveryRunning = true
        status = STATUS_CHECKING
        currentSsid = ""
        connectedSsid = ""
        NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_START")

        thread(name = "TawkitWifiRecovery") {
            try {
                forceAutoTimeSync(context)

                if (isNetworkConnected(context)) {
                    status = STATUS_CONNECTED
                    NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_ALREADY_CONNECTED")
                    return@thread
                }

                if (!DeviceOwnerInstaller.isDeviceOwner(context)) {
                    status = STATUS_UNSUPPORTED
                    NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_UNSUPPORTED reason=not_device_owner")
                    return@thread
                }

                status = STATUS_TRYING
                val wifiManager = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager

                // Meme depreciation/meme exception Device Owner que enableNetwork()
                // ci-dessous (deja verifiee juste au-dessus) : sans ca, une box dont
                // la radio Wi-Fi est eteinte (cas reel constate en test, 11/08/2026)
                // ne tenterait jamais rien -- enableNetwork()/reconnect() sur une
                // radio eteinte n'a aucun effet.
                @Suppress("DEPRECATION")
                if (!wifiManager.isWifiEnabled) {
                    try { wifiManager.isWifiEnabled = true } catch (e: Exception) {
                        Log.e("TWKT", "WifiRecoveryHelper: setWifiEnabled error: ${e.message}")
                    }
                    Thread.sleep(2000)   // laisse la radio demarrer avant le 1er enableNetwork()
                }

                @Suppress("DEPRECATION")
                val configs = try { wifiManager.configuredNetworks } catch (e: Exception) { null }

                if (configs.isNullOrEmpty()) {
                    status = STATUS_FAILED
                    NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_FAILED reason=no_saved_networks")
                    return@thread
                }

                @Suppress("DEPRECATION")
                for (config in configs) {
                    currentSsid = stripQuotes(config.SSID)
                    NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_TRY_NETWORK ssid=$currentSsid id=${config.networkId}")
                    try {
                        wifiManager.enableNetwork(config.networkId, true)
                        wifiManager.reconnect()
                    } catch (e: Exception) {
                        Log.e("TWKT", "WifiRecoveryHelper: enableNetwork error: ${e.message}")
                        continue
                    }

                    var waited = 0L
                    while (waited < PER_NETWORK_TIMEOUT_MS) {
                        if (isNetworkConnected(context)) {
                            status = STATUS_CONNECTED
                            connectedSsid = currentSsid
                            NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_CONNECTED ssid=$connectedSsid id=${config.networkId}")
                            return@thread
                        }
                        Thread.sleep(POLL_STEP_MS)
                        waited += POLL_STEP_MS
                    }
                }

                currentSsid = ""
                status = STATUS_FAILED
                NativeEventLog.log(context, "SYS", "WIFI_RECOVERY_FAILED reason=all_networks_exhausted")
            } catch (e: Exception) {
                Log.e("TWKT", "WifiRecoveryHelper: startRecovery error: ${e.message}")
                status = STATUS_FAILED
            } finally {
                recoveryRunning = false
            }
        }
    }

    /**
     * Infos reseau courantes (type/SSID/IP) -- pollees par custom.js pour le
     * clic sur l'icone Wi-Fi (remplace l'etoile verte/rouge coeur, cf.
     * internetStatusIndicatorVertical/Horizontal, index.html) : affiche le
     * SSID connecte + l'IP de la box a l'utilisateur.
     */
    fun getNetworkInfo(context: Context): String {
        val result = JSONObject()
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = network?.let { cm.getNetworkCapabilities(it) }
            val linkProps = network?.let { cm.getLinkProperties(it) }

            val connected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val type = when {
                caps == null -> "none"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                else -> "other"
            }

            var ssid = ""
            if (type == "wifi") {
                try {
                    @Suppress("DEPRECATION")
                    val wifiManager = context.applicationContext
                        .getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val raw = stripQuotes(wifiManager.connectionInfo?.ssid)
                    if (raw != "<unknown ssid>") ssid = raw
                } catch (e: Exception) {
                    Log.e("TWKT", "WifiRecoveryHelper: ssid read error: ${e.message}")
                }
            }

            val ip = linkProps?.linkAddresses
                ?.map { it.address.hostAddress ?: "" }
                ?.firstOrNull { it.isNotEmpty() && !it.contains(":") }   // IPv4 en priorite, plus lisible
                ?: linkProps?.linkAddresses?.firstOrNull()?.address?.hostAddress ?: ""

            result.put("connected", connected)
            result.put("type", type)
            result.put("ssid", ssid)
            result.put("ip", ip)
        } catch (e: Exception) {
            Log.e("TWKT", "WifiRecoveryHelper: getNetworkInfo error: ${e.message}")
            result.put("connected", false)
            result.put("type", "none")
            result.put("ssid", "")
            result.put("ip", "")
        }
        return result.toString()
    }

    fun openWifiSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e("TWKT", "WifiRecoveryHelper: openWifiSettings failed: ${e.message}")
        }
    }

    /**
     * Force le fuseau horaire systeme via DevicePolicyManager.setTimeZone() --
     * meme contrainte Device Owner que forceAutoTimeSync() ci-dessus (aucune
     * app tierce ordinaire ne peut changer le fuseau systeme). Utilise pour
     * les box dont le pays (MOSQUE_CONFIG.LOCATION_CODE, verifie cote
     * custom.js) est la Tunisie : le tzdata Android embarque sur ces box bas
     * de gamme applique parfois a tort une regle DST a Africa/Tunis, ce qui
     * decale l'heure affichee. Africa/Brazzaville (Congo) est UTC+1 fixe,
     * sans aucune regle DST, et correspond a l'heure civile tunisienne
     * reelle depuis l'abandon du changement d'heure -- demande explicite du
     * 11/08/2026. Ne fait rien si le fuseau courant est deja celui demande
     * (evite un appel systeme inutile a chaque verification periodique cote
     * custom.js).
     *
     * DevicePolicyManager.setTimeZone() echoue silencieusement (retourne
     * false) tant que Settings.Global.AUTO_TIME_ZONE est actif (documente
     * Android : la detection reseau/GPS du fuseau est alors prioritaire).
     * forceAutoTimeSync() ci-dessus active ce reglage pendant une reprise
     * Wi-Fi -- il faut donc explicitement le desactiver ici pour que le
     * forcage Brazzaville tienne. AUTO_TIME (date/heure, distinct du fuseau)
     * reste lui active : la box continue de se resynchroniser via NTP,
     * seul le choix du fuseau est fige.
     */
    fun applyTimeZoneOverride(context: Context, tzId: String): Boolean {
        if (!DeviceOwnerInstaller.isDeviceOwner(context)) return false
        return try {
            if (java.util.TimeZone.getDefault().id == tzId) return false
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(context, TawkitDeviceAdminReceiver::class.java)
            dpm.setGlobalSetting(admin, Settings.Global.AUTO_TIME_ZONE, "0")
            val ok = dpm.setTimeZone(admin, tzId)
            if (ok) {
                NativeEventLog.log(context, "SYS", "TIMEZONE_OVERRIDE_APPLIED tz=$tzId")
            } else {
                NativeEventLog.log(context, "SYS", "TIMEZONE_OVERRIDE_REJECTED tz=$tzId")
            }
            ok
        } catch (e: Exception) {
            Log.e("TWKT", "WifiRecoveryHelper: applyTimeZoneOverride error: ${e.message}")
            false
        }
    }
}
