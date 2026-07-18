package net.tawkit.mobile

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Gere le stockage local des recitateurs Coran (hors APK) :
 *   - dossier prive de l'app : getExternalFilesDir(null)/reciters/<id>/NNN.ogg
 *   - import depuis une cle USB via SAF (Storage Access Framework)
 *   - listing / suppression pour le pont JS (MobileJsBridge)
 *
 * Format attendu sur la cle USB (ou dans l'archive ZIP telechargee) :
 * OBLIGATOIREMENT sous un dossier nomme "CORAN" (insensible a la casse) —
 * jamais de recitateur directement a la racine du support choisi :
 *
 *   <cle USB>/
 *     CORAN/
 *       Nom_Du_Recitateur_1/
 *         meta.json        (optionnel : {"name": "Cheikh ..."})
 *         001.ogg
 *         002.ogg
 *         ...
 *         114.ogg
 *       Nom_Du_Recitateur_2/
 *         ...
 *
 * Dans le picker SAF, l'utilisateur peut choisir soit le dossier "CORAN"
 * lui-meme, soit son dossier parent (ex. la racine de la cle USB) : si le
 * dossier choisi n'est pas "CORAN", importFromTree() recherche un sous-dossier
 * "CORAN" et descend automatiquement dedans. Si aucun "CORAN" n'est trouve,
 * l'import echoue avec un message explicite (pas de fallback silencieux sur
 * la racine).
 *
 * Si le dossier "CORAN" contient directement des fichiers NNN.ogg (pas de
 * sous-dossiers), il est traite comme un seul recitateur (structure plate).
 */
object ReciterManager {

    private const val DIR_NAME = "reciters"
    private val AUDIO_REGEX = Regex("""^(\d{3})\.(ogg|mp3)$""", RegexOption.IGNORE_CASE)

    fun getBaseDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sanitizeId(name: String): String {
        val cleaned = name.trim().lowercase()
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .trim('_')
        return cleaned.ifEmpty { "reciter_${System.currentTimeMillis()}" }
    }

    /** Liste les recitateurs deja installes en local (USB ou telecharges). JSON string. */
    fun listReciters(context: Context): String {
        val baseDir = getBaseDir(context)
        val result = JSONArray()
        baseDir.listFiles { f -> f.isDirectory }?.sortedBy { it.name }?.forEach { dir ->
            val trackCount = dir.listFiles { f -> AUDIO_REGEX.matches(f.name) }?.size ?: 0
            if (trackCount > 0) {
                // "name" peut etre soit une simple chaine, soit un objet par
                // langue ex. {"AR":"...","FR":"...","EN":"..."} — on transmet
                // la valeur brute telle quelle a JS, qui choisit la langue
                // courante de l'UI (cf. _localizedReciterName() dans custom.js).
                // Une simple chaine reste donc parfaitement valide (retro-compat).
                var nameVal: Any = dir.name
                val metaFile = File(dir, "meta.json")
                if (metaFile.exists()) {
                    try {
                        nameVal = JSONObject(metaFile.readText()).opt("name") ?: dir.name
                    } catch (e: Exception) { /* meta.json invalide -> garder le nom du dossier */ }
                }
                result.put(JSONObject().apply {
                    put("id", dir.name)
                    put("name", nameVal)
                    put("trackCount", trackCount)
                })
            }
        }
        return result.toString()
    }

    fun removeReciter(context: Context, id: String): Boolean {
        val dir = File(getBaseDir(context), sanitizeId(id))
        return !dir.exists() || dir.deleteRecursively()
    }

    /** URL file:// du fichier audio pour un recitateur+sourate, ou "" si absent. */
    fun getAudioUrl(context: Context, id: String, surahNum: Int): String {
        val dir = File(getBaseDir(context), sanitizeId(id))
        val padded = String.format("%03d", surahNum)
        for (ext in listOf("ogg", "mp3")) {
            val f = File(dir, "$padded.$ext")
            if (f.exists()) return "file://" + f.absolutePath
        }
        return ""
    }

    // ── Import USB / SAF ───────────────────────────────────────────────────
    // Etat pollable depuis JS (getImportStatus) car le picker SAF + la copie
    // sont asynchrones et n'ont pas de canal de retour direct vers la page.

    @Volatile private var importStatus: String = "idle"      // idle | copying | done | error
    @Volatile private var importMessage: String = ""
    @Volatile private var importedCount: Int = 0

    fun getImportStatus(): String = JSONObject().apply {
        put("status", importStatus)
        put("message", importMessage)
        put("importedCount", importedCount)
    }.toString()

    /** A appeler depuis une coroutine (IO), apres que l'utilisateur a choisi un dossier via SAF. */
    fun importFromTree(context: Context, treeUri: Uri) {
        importStatus = "copying"
        importMessage = ""
        importedCount = 0
        try {
            val picked = DocumentFile.fromTreeUri(context, treeUri)
            if (picked == null || !picked.isDirectory) {
                importStatus = "error"; importMessage = "Dossier invalide"; return
            }

            // La structure est obligatoirement sous un dossier nomme "CORAN"
            // (insensible a la casse) : l'utilisateur peut choisir ce dossier
            // directement, ou son dossier parent (ex. la racine de la cle USB),
            // auquel cas on descend automatiquement dans "CORAN". Aucun
            // recitateur ne doit etre lu directement a la racine du support.
            val root: DocumentFile = if (picked.name?.equals("CORAN", ignoreCase = true) == true) {
                picked
            } else {
                val coranDir = picked.listFiles().firstOrNull {
                    it.isDirectory && it.name?.equals("CORAN", ignoreCase = true) == true
                }
                if (coranDir == null) {
                    importStatus = "error"
                    importMessage = "Dossier \"CORAN\" introuvable. Choisissez le dossier CORAN " +
                        "ou son dossier parent sur la cle USB (structure attendue : " +
                        ".../CORAN/<recitateur>/001.ogg...114.ogg)"
                    return
                }
                coranDir
            }

            val baseDir = getBaseDir(context)
            val children = root.listFiles()
            val reciterDirs = children.filter { it.isDirectory }

            val sources = if (reciterDirs.isNotEmpty()) {
                reciterDirs
            } else if (children.any { isAudioFileName(it.name) }) {
                listOf(root) // structure plate : CORAN/ contient directement les pistes (un seul recitateur)
            } else {
                emptyList()
            }

            if (sources.isEmpty()) {
                importStatus = "error"
                importMessage = "Aucun recitateur trouve dans CORAN (dossiers attendus avec 001.ogg...114.ogg)"
                return
            }

            for (src in sources) {
                val rawName = src.name ?: continue
                val id = sanitizeId(rawName)
                importMessage = rawName
                val targetDir = File(baseDir, id)
                if (!targetDir.exists()) targetDir.mkdirs()
                src.listFiles().forEach { file ->
                    val fname = file.name ?: return@forEach
                    if (isAudioFileName(fname) || fname.equals("meta.json", ignoreCase = true)) {
                        context.contentResolver.openInputStream(file.uri)?.use { input ->
                            File(targetDir, fname).outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                importedCount++
            }
            importStatus = "done"
            importMessage = "$importedCount recitateur(s) importe(s)"
        } catch (e: Exception) {
            importStatus = "error"
            importMessage = e.message ?: "Erreur import"
        }
    }

    private fun isAudioFileName(name: String?): Boolean = name != null && AUDIO_REGEX.matches(name)
}
