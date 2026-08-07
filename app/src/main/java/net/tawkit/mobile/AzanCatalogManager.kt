package net.tawkit.mobile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Stockage natif des azans du catalogue en ligne (spec/audio/azan/azan-catalog.json) :
 *   getExternalFilesDir(null)/azan_catalog/<id>.<ext>
 *
 * Remplace l'ancien cache IndexedDB (WebView) : un fichier sur disque, scanné
 * à chaque ouverture du catalogue, est desormais le seul état "installé" —
 * même principe que ReciterManager (aucun flag séparé pouvant se désynchroniser
 * du contenu réel du disque, et robuste à une mise à jour de l'appli).
 */
object AzanCatalogManager {

    private const val DIR_NAME = "azan_catalog"

    private data class DlState(val status: String, val message: String = "")
    private val downloads = ConcurrentHashMap<String, DlState>()

    fun getBaseDir(context: Context): java.io.File {
        val dir = java.io.File(context.getExternalFilesDir(null), DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun fileFor(context: Context, id: String): java.io.File? =
        getBaseDir(context).listFiles { f -> f.isFile && !f.name.endsWith(".part") && f.nameWithoutExtension == id }
            ?.firstOrNull()

    /** Fichier réellement présent sur disque pour cet id, ou null si jamais
     *  téléchargé (ou supprimé) -- utilisé par AzanPlaybackService pour jouer
     *  nativement le muezzin choisi dans le catalogue au lieu du son par défaut. */
    fun getInstalledFile(context: Context, id: String): java.io.File? = fileFor(context, id)

    /** Ids déjà présents sur le disque (scan direct, pas d'état séparé). JSON: ["id1","id2",...] */
    fun listInstalledIds(context: Context): String {
        val ids = getBaseDir(context).listFiles { f -> f.isFile && !f.name.endsWith(".part") }
            ?.map { it.nameWithoutExtension } ?: emptyList()
        return JSONArray(ids).toString()
    }

    /** URL file:// jouable directement par <audio>, ou "" si absent localement. */
    fun getFileUrl(context: Context, id: String): String {
        val f = fileFor(context, id) ?: return ""
        return "file://" + f.absolutePath
    }

    /**
     * Contenu brut du catalogue bundlé (spec/audio/azan/azan-catalog.json),
     * lu directement via AssetManager plutôt que fetch()/XHR côté JS : sur
     * certains WebView (constaté sur boîtier Android TV), fetch() vers une
     * ressource file:// locale échoue purement et simplement ("Failed to
     * fetch"), y compris avec allowFileAccessFromFileURLs — même limitation
     * que le fetch() cross-origin déjà documentée pour http://127.0.0.1:8080
     * (cf. AzanCatalogManager/CLAUDE.md). Lire l'asset nativement contourne
     * totalement cette restriction, quel que soit le moteur du WebView.
     * Retourne "" si l'asset est introuvable/illisible (JS bascule alors sur
     * son propre repli fetch()).
     */
    fun readBundledCatalogJson(context: Context): String {
        return try {
            context.assets.open("spec/audio/azan/azan-catalog.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    fun getDownloadStatus(id: String): String {
        val st = downloads[id] ?: return JSONObject().apply { put("status", "idle") }.toString()
        return JSONObject().apply {
            put("status", st.status)
            if (st.message.isNotEmpty()) put("message", st.message)
        }.toString()
    }

    /** Télécharge en tâche de fond (coroutine IO) ; l'état est ensuite lu via getDownloadStatus(id). */
    fun startDownload(context: Context, id: String, url: String) {
        if (downloads[id]?.status == "downloading") return
        downloads[id] = DlState("downloading")
        CoroutineScope(Dispatchers.IO).launch {
            var conn: HttpURLConnection? = null
            try {
                val ext = url.substringAfterLast('.', "ogg").substringBefore('?').take(4).ifEmpty { "ogg" }
                val baseDir = getBaseDir(context)
                val target = java.io.File(baseDir, "$id.$ext")
                val part = java.io.File(baseDir, "$id.$ext.part")

                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    connect()
                }
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    downloads[id] = DlState("error", "HTTP ${conn.responseCode}")
                    return@launch
                }
                part.outputStream().use { out -> conn.inputStream.use { it.copyTo(out) } }
                if (target.exists()) target.delete()
                part.renameTo(target)
                downloads[id] = DlState("done")
            } catch (e: Exception) {
                downloads[id] = DlState("error", e.message ?: "erreur réseau")
            } finally {
                conn?.disconnect()
            }
        }
    }

    // ── Fichier personnalisé choisi par l'utilisateur (explorateur Android,
    // cf. custom.js _acPickCustomFile / MobileJsBridge.pickCustomAzanFile) ────
    // Réutilise EXACTEMENT le même stockage/lookup que le catalogue en ligne
    // (fichier sous azan_catalog/<id>.<ext>, id = clé unique réservée par
    // groupe) : AzanPlaybackService.playAzan() et _acApplyAzanToPlayer (JS)
    // n'ont donc besoin d'AUCUNE modification pour jouer un fichier importé
    // par l'utilisateur -- ils ne savent pas (et n'ont pas besoin de savoir)
    // que cet id ne vient pas du catalogue distant.
    const val CUSTOM_FAJR_ID    = "custom_fajr"
    const val CUSTOM_GENERAL_ID = "custom_general"
    private const val PREFS_NAME_CUSTOM  = "tawkit_azan_custom_prefs"
    private const val PREF_NAME_PREFIX   = "custom_name_"   // + groupKey
    private val SUPPORTED_EXT = setOf("mp3", "ogg", "oga", "mp4", "m4a", "wav", "aac")

    fun customIdFor(groupKey: String): String = if (groupKey == "fajr") CUSTOM_FAJR_ID else CUSTOM_GENERAL_ID

    private data class ImportState(val status: String, val message: String = "")
    private val customImports = ConcurrentHashMap<String, ImportState>()   // groupKey -> state

    fun getCustomImportStatus(groupKey: String): String {
        val st = customImports[groupKey] ?: return JSONObject().apply { put("status", "idle") }.toString()
        return JSONObject().apply {
            put("status", st.status)
            if (st.message.isNotEmpty()) put("message", st.message)
        }.toString()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) { null }
    }

    /** A appeler depuis une coroutine (IO), apres que l'utilisateur a choisi un
     *  fichier via le picker SAF (ActivityResultContracts.OpenDocument). */
    fun importCustomFile(context: Context, groupKey: String, uri: Uri) {
        customImports[groupKey] = ImportState("copying")
        try {
            val displayName = queryDisplayName(context, uri) ?: "audio"
            var ext = displayName.substringAfterLast('.', "").lowercase()
            if (ext !in SUPPORTED_EXT) {
                ext = when (context.contentResolver.getType(uri)) {
                    "audio/mpeg" -> "mp3"
                    "audio/mp4", "audio/m4a" -> "m4a"
                    "audio/aac" -> "aac"
                    "audio/wav", "audio/x-wav" -> "wav"
                    else -> "ogg"
                }
            }
            val id = customIdFor(groupKey)
            val baseDir = getBaseDir(context)
            // Supprime tout fichier precedent pour ce groupe (extension possiblement differente)
            baseDir.listFiles { f -> f.isFile && f.nameWithoutExtension == id }?.forEach { it.delete() }
            val target = java.io.File(baseDir, "$id.$ext")
            val input = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Impossible de lire le fichier choisi")
            input.use { inp -> target.outputStream().use { out -> inp.copyTo(out) } }

            context.getSharedPreferences(PREFS_NAME_CUSTOM, Context.MODE_PRIVATE).edit()
                .putString(PREF_NAME_PREFIX + groupKey, displayName)
                .apply()

            customImports[groupKey] = ImportState("done")
        } catch (e: Exception) {
            customImports[groupKey] = ImportState("error", e.message ?: "erreur de copie")
        }
    }

    /** JSON: {hasFile, fileName} -- fileName = nom d'origine choisi par l'utilisateur. */
    fun getCustomFileInfo(context: Context, groupKey: String): String {
        val id = customIdFor(groupKey)
        val f = getInstalledFile(context, id)
        val name = context.getSharedPreferences(PREFS_NAME_CUSTOM, Context.MODE_PRIVATE)
            .getString(PREF_NAME_PREFIX + groupKey, "") ?: ""
        return JSONObject().apply {
            put("hasFile", f != null && f.exists())
            put("fileName", name)
        }.toString()
    }

    fun clearCustomFile(context: Context, groupKey: String) {
        val id = customIdFor(groupKey)
        getBaseDir(context).listFiles { f -> f.isFile && f.nameWithoutExtension == id }?.forEach { it.delete() }
        context.getSharedPreferences(PREFS_NAME_CUSTOM, Context.MODE_PRIVATE).edit()
            .remove(PREF_NAME_PREFIX + groupKey)
            .apply()
        customImports.remove(groupKey)
    }
}
