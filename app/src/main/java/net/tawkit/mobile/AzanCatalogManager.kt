package net.tawkit.mobile

import android.content.Context
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
}
