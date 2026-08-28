package net.tawkit.mobile

import android.app.UiModeManager
import android.content.Context
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
        return uiModeIsTv || hasLeanback || noTouchscreen
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

    // Surcharges manuelles (posees par adb sur un boitier precis, cf.
    // z6-aboubaker : `setprop persist.tawkit.gpu_hw_ok 1`) :
    //   persist.tawkit.gpu_hw_ok = 1  -> FORCE le rendu GPU (LAYER_TYPE_HARDWARE)
    //     meme sur un Mali-G31. Certaines revisions firmware/pilote (ex. Allwinner
    //     H618 Mali-G31 r0p0, pilote r20p0) n'ont PAS le hang JOB_READ_FAULT du
    //     X96Q_PRO1 (H616) -- verifie stable en soak + cycle azan complet le
    //     28/08/2026, gain marquee 200ms -> 29ms/frame. A poser uniquement apres
    //     verification sur CE boitier.
    //   persist.tawkit.gpu_force_sw = 1 -> FORCE le rendu logiciel (echappatoire
    //     si un boitier hors Mali-G31 montre le meme hang).
    private val overrideCache: Boolean? by lazy {
        when {
            sysProp("persist.tawkit.gpu_force_sw") == "1" -> true
            sysProp("persist.tawkit.gpu_hw_ok")    == "1" -> false
            else -> null
        }
    }

    // Resultat mis en cache (lazy, un seul acces disque par process) : appele
    // a la fois par MainActivity.setupWebView() et par MobileJsBridge cote JS
    // (custom.js, mode marquee fige), doit rester bon marche a chaque appel.
    fun isKnownBuggyGpu(): Boolean = overrideCache ?: knownBuggyGpuCache
}
