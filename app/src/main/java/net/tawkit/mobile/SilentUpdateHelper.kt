package net.tawkit.mobile

import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Installation d'APK sans passer par l'installateur système (aucune boîte de
 * confirmation) sur les box dont le firmware expose un accès root réellement
 * utilisable PAR LE PROCESS DE L'APP ELLE-MÊME (pas juste via adb).
 *
 * CORRECTIF IMPORTANT (31/07/2026) : une première vérification manuelle
 * ("adb root" puis "su <uid app> su 0 id") avait conclu, à tort, que le
 * process de l'app pourrait obtenir root de la même façon. Le vrai test --
 * cette classe, exécutée par l'app elle-même, pas par un shell adb -- échoue
 * systématiquement avec "su: setgid failed: Operation not permitted" (cf.
 * logcat, X88 Pro 20). Cause réelle : un process d'app (forké par Zygote,
 * domaine SELinux untrusted_app) est dépouillé des capabilities Linux
 * (CAP_SETUID/CAP_SETGID) nécessaires pour que /system/xbin/su réussisse ses
 * setuid()/setgid() -- alors qu'un process issu du shell adb (uid=2000) les
 * conserve. "adb root"/"adb shell su <uid>" ne prouve donc RIEN sur ce que le
 * process réel de l'app peut faire : le chaînage était resté dans le domaine
 * SELinux "su" du début à la fin, jamais dans "untrusted_app" comme l'app
 * réelle. C'est un mur de sécurité Android standard (pas une particularité de
 * ce boîtier) -- checkCapability() ci-dessous renverra donc très probablement
 * false sur la quasi-totalité des box, ce firmware inclus. Conservé tel quel
 * (probe réelle, jamais supposée) car (a) certains firmwares tiers mal
 * configurés peuvent réellement laisser passer un su non restrictif, et (b)
 * l'appelant DOIT de toute façon prévoir un repli (cf.
 * AppUpdateDownloader.installApk, 1 tap) qui reste la voie normale.
 */
object SilentUpdateHelper {

    // Chemin absolu obligatoire -- constaté en conditions réelles (31/07/2026) :
    // un ProcessBuilder("su", ...) avec un nom nu échoue silencieusement
    // (IOException avalée par le catch) alors que "su 10067 id" réussit
    // parfaitement en shell adb. Cause : le process d'une app (forké depuis
    // Zygote) n'hérite pas forcément d'un PATH incluant /system/xbin, contrairement
    // à un shell adb -- la résolution par nom nu ne peut donc pas être fiable.
    // Liste ordonnée par emplacement le plus courant sur les box testées ; le
    // premier chemin existant est utilisé, jamais de recherche par PATH.
    private val SU_CANDIDATES = listOf(
        "/system/xbin/su", "/system/bin/su", "/su/bin/su", "/sbin/su", "/vendor/bin/su"
    )

    private fun resolveSuPath(): String? = SU_CANDIDATES.firstOrNull { File(it).exists() }

    /**
     * Sonde en lecture seule : ne modifie rien sur l'appareil, vérifie
     * uniquement qu'un accès root est disponible pour l'UID de CETTE app
     * (pas celui du shell adb -- root via adb ne prouve rien pour l'app
     * elle-même, cf. investigation manuelle qui a précédé ce code).
     */
    fun checkCapability(): Boolean {
        // Log.d à chaque branche (exception, timeout, code de sortie, sortie
        // texte) : utile pour comprendre à distance pourquoi une box donnée
        // est/n'est pas capable, sans avoir à s'y déplacer (cf. commentaire
        // de classe ci-dessus -- consultable via l'action distante send_debug
        // si un jour ce niveau de détail est aussi voulu dans le rapport
        // envoyé au développeur, pas le cas actuellement : Log.d va au logcat
        // natif, pas à _dbgLogs cote JS).
        val suPath = resolveSuPath()
        if (suPath == null) {
            Log.d("TWKT", "SilentUpdateHelper: no su binary found in $SU_CANDIDATES")
            return false
        }
        return try {
            val process = ProcessBuilder(suPath, "0", "id").redirectErrorStream(true).start()
            process.outputStream.close()   // EOF immédiat sur stdin -- au cas où su attend une entrée
            val finished = process.waitFor(3, TimeUnit.SECONDS)
            if (!finished) {
                Log.d("TWKT", "SilentUpdateHelper: checkCapability TIMEOUT ($suPath)")
                process.destroyForcibly()
                return false
            }
            val output = process.inputStream.bufferedReader().readText()
            val code = process.exitValue()
            Log.d("TWKT", "SilentUpdateHelper: checkCapability exit=$code output='${output.trim()}'")
            code == 0 && output.contains("uid=0")
        } catch (e: Exception) {
            Log.d("TWKT", "SilentUpdateHelper: checkCapability EXCEPTION ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    /**
     * Installe l'APK déjà téléchargé sur disque, sans confirmation. Timeout
     * large (60s) : contrairement à checkCapability(), pm install effectue
     * une vérification/dexopt réelle sur un APK d'~80 Mo, pas juste un `id`.
     */
    fun installSilently(apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        val suPath = resolveSuPath() ?: return false
        return try {
            val process = ProcessBuilder(suPath, "0", "pm", "install", "-r", apkFile.absolutePath)
                .redirectErrorStream(true).start()
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return false
            }
            val output = process.inputStream.bufferedReader().readText()
            process.exitValue() == 0 && output.contains("Success")
        } catch (e: Exception) {
            false
        }
    }
}
