package net.tawkit.mobile

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import java.io.File

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
        // 4e signal (ajoute 11/09/2026, boitier aboubakr Z6/"Oranth") : ce
        // boitier declare FEATURE_TOUCHSCREEN=true (flag ROM generique copie
        // d'une base telephone, alors qu'il n'y a physiquement aucun
        // digitiseur) ET pas de FEATURE_LEANBACK ET un UI mode pas TELEVISION
        // -- les 3 signaux existants se trompaient donc tous a la fois sur ce
        // boitier reel, DeviceType.isAndroidTv() renvoyait false et TOUTES les
        // protections TV (mode kiosque onStop, relance TV au boot, reassert
        // launcher d'accueil...) restaient inactives, laissant ce boitier se
        // comporter comme un telephone. Absence de batterie est en revanche
        // une verite materielle que ces ROMs generiques ne "trichent" pas --
        // aucun boitier mural mosquee n'a de batterie, tout telephone/tablette
        // en a une (meme les rares tablettes "toujours branchees" en
        // rapportent une, juste chargee a 100%).
        val noBattery = try {
            val batteryStatus = context.registerReceiver(
                null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            batteryStatus?.getBooleanExtra(android.os.BatteryManager.EXTRA_PRESENT, true) == false
        } catch (e: Exception) {
            false // signal indisponible -> ne penalise pas les 3 autres, comme avant
        }
        return uiModeIsTv || hasLeanback || noTouchscreen || noBattery
    }

    // Chemin standard du pilote noyau ARM Mali "kbase" (Midgard/Bifrost),
    // commun a la quasi-totalite des boitiers TV bon marche quel que soit le
    // vendeur du SoC (Allwinner/Amlogic/Rockchip licencient tous le meme
    // pilote de reference) -- expose en lecture seule (r--r--r--, verifie
    // sur boitier 192.168.1.210), lisible SANS root par n'importe quelle app.
    private const val MALI_GPUINFO_PATH = "/sys/class/misc/mali0/device/gpuinfo"

    // Puces Mali confirmees instables sous rendu GPU soutenu (page fault /
    // hang complet du thread GPU Chromium, JOB_READ_FAULT dans dmesg) :
    // diagnostique boitier 192.168.1.210 (X96Q_PRO1, Allwinner H616,
    // Mali-G31) le 24-25/08/2026 -- plantage constate meme hors azan (simple
    // redessin de l'horloge/compteur chaque seconde suffit), aucune
    // isolation CSS (translateZ layers) ni ajustement applicatif ne l'evite,
    // seul un WebView 100% logiciel (LAYER_TYPE_SOFTWARE) l'elimine. Ajouter
    // ici tout autre modele Mali qui montrerait le meme comportement.
    private val KNOWN_BUGGY_GPU_MARKERS = listOf("Mali-G31")

    private val knownBuggyGpuCache: Boolean by lazy {
        try {
            val gpuInfo = File(MALI_GPUINFO_PATH).readText()
            KNOWN_BUGGY_GPU_MARKERS.any { gpuInfo.contains(it, ignoreCase = true) }
        } catch (e: Exception) {
            false   // fichier absent/illisible (autre pilote GPU, ex. TVBOX/ohm Amlogic) -> pas de correspondance, rendu GPU normal
        }
    }

    /** Lecture d'une propriete systeme (android.os.SystemProperties, @hide mais
     *  API stable) sans dependance -- repli "" si indisponible. */
    private fun sysProp(key: String): String = try {
        @Suppress("PrivateApi")
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, key) as? String ?: ""
    } catch (e: Exception) { "" }

    // Ordre de decision de isKnownBuggyGpu(context) ci-dessous :
    //
    //   1. persist.tawkit.gpu_force_sw = 1  (adb, echappatoire manuelle)
    //        -> FORCE le rendu logiciel, quel que soit le GPU.
    //   2. GpuRecovery.shouldForceSoftware(context)  (auto-apprentissage)
    //        -> le compositeur a deja gele plusieurs fois sur CE boitier et le
    //           watchdog a du redemarrer le process : on se rabat sur le rendu
    //           logiciel tout seul, sans intervention adb. Sticky.
    //   3. persist.tawkit.gpu_hw_ok = 1  (adb, cf. z6-aboubaker)
    //        -> FORCE le rendu GPU (LAYER_TYPE_HARDWARE) meme sur un Mali-G31.
    //           Certaines revisions firmware/pilote n'ont pas le hang du
    //           X96Q_PRO1 (H616). A poser uniquement apres verification soak
    //           sur CE boitier -- et le point 2 peut quand meme reprendre la
    //           main si le materiel finit par lacher (le latch GpuRecovery
    //           l'emporte volontairement sur ce flag).
    //   4. sinon : liste de puces connues instables (Mali-G31).
    private fun manualForceSoftware(): Boolean = sysProp("persist.tawkit.gpu_force_sw") == "1"
    private fun manualForceHardware(): Boolean = sysProp("persist.tawkit.gpu_hw_ok") == "1"

    // Appele par MainActivity.setupWebView(). Doit rester bon marche : les
    // sysProp sont des appels reflechis legers, knownBuggyGpuCache est lazy
    // (un seul acces disque /sys par process), et GpuRecovery lit un
    // SharedPreferences deja monte.
    fun isKnownBuggyGpu(context: Context): Boolean = when {
        manualForceSoftware() -> true
        GpuRecovery.shouldForceSoftware(context) -> true
        manualForceHardware() -> false
        else -> knownBuggyGpuCache
    }
}
