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
 * Stockage natif des textes de riwaya (Hafs/Qaloun/Warsh, ~1.3 Mo chacun) :
 *   getExternalFilesDir(null)/riwayat/<id>.json
 *
 * Remplace l'ancien cache IndexedDB + le flag JS_CUSTOM.ucRiwayaInstalled
 * (localStorage) : un fichier sur disque, scanné à chaque ouverture du
 * lecteur, est desormais le seul état "installé" — même principe que
 * ReciterManager/AzanCatalogManager.
 */
object RiwayaManager {

    private const val DIR_NAME = "riwayat"

    private data class DlState(val status: String, val message: String = "")
    private val downloads = ConcurrentHashMap<String, DlState>()

    fun getBaseDir(context: Context): java.io.File {
        val dir = java.io.File(context.getExternalFilesDir(null), DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun fileFor(context: Context, id: String): java.io.File =
        java.io.File(getBaseDir(context), "$id.json")

    /** Ids déjà présents sur le disque (scan direct, pas d'état séparé). JSON: ["hafs","warsh",...] */
    fun listInstalledIds(context: Context): String {
        val ids = getBaseDir(context).listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.map { it.nameWithoutExtension } ?: emptyList()
        return JSONArray(ids).toString()
    }

    /**
     * Contenu texte (JSON) du fichier, ou "" si absent localement.
     *
     * Renvoyé directement en String plutôt qu'une URL file:// à charger via
     * fetch() : l'API Fetch de Chromium/WebView rejette le schéma file://
     * côté client avant même de tenter quoi que ce soit (TypeError: Failed to
     * fetch, sans aucune requête réseau ni trace dans la console) — contrairement
     * aux éléments média (<audio src="file://...">, cf. ReciterManager/
     * AzanCatalogManager) qui chargent le file:// sans problème car ce n'est
     * pas le même mécanisme de chargement.
     */
    fun readContent(context: Context, id: String): String {
        val f = fileFor(context, id)
        if (!f.exists()) return ""
        return try { f.readText(Charsets.UTF_8) } catch (e: Exception) { "" }
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
                val target = fileFor(context, id)
                val part = java.io.File(getBaseDir(context), "$id.json.part")

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
