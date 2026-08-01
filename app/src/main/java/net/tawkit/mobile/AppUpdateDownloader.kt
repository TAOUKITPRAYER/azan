package net.tawkit.mobile

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Télécharge l'APK de mise à jour en HTTP direct (HttpURLConnection, même
 * mécanisme que AppUpdateChecker.fetchRemoteVersion), avec suivi manuel des
 * redirections (Location header) plutôt que instanceFollowRedirects.
 *
 * Remplace l'ancienne implémentation basée sur android.app.DownloadManager
 * le 31/07/2026, après avoir constaté en conditions réelles (box mediouni)
 * un échec systématique avec DownloadManager.COLUMN_REASON=404. CAUSE
 * RÉELLE identifiée ensuite (voir commentaire de downloadTo ci-dessous) :
 * absence d'en-tête User-Agent sur la requête -- probablement filtrée par
 * GitHub/son CDN sur ce point d'accès précis, indépendamment de
 * DownloadManager ou d'HttpURLConnection. La bascule vers une implémentation
 * HTTP directe n'était donc pas strictement nécessaire pour corriger le 404
 * (ajouter le même en-tête à DownloadManager.Request aurait sans doute
 * suffi), mais est conservée : meilleure visibilité (code HTTP, reason
 * exacte) et aucune dépendance à un composant système tiers.
 *
 * Les constantes DownloadManager.STATUS_* sont réutilisées telles quelles
 * (ce sont de simples int) pour ne rien changer à l'interface publique --
 * RemoteSilentUpdater et UpdateProgressDialog continuent de fonctionner sans
 * modification.
 *
 * Un seul téléchargement à la fois (comme l'implémentation DownloadManager
 * précédente, qui ne suivait elle aussi qu'un pending_download_id global).
 */
object AppUpdateDownloader {

    private const val PREFS_NAME = "tawkit_update_prefs"
    private const val PREF_DOWNLOAD_ID = "pending_download_id"
    private const val APK_FILE_NAME = "taoukit_update.apk"

    data class DownloadProgress(val status: Int, val bytesDownloaded: Long, val totalBytes: Long, val reason: Int)

    private class HttpDownloadException(val httpCode: Int, message: String) : Exception(message)

    @Volatile private var currentId: Long = -1L
    @Volatile private var currentStatus: Int = DownloadManager.STATUS_RUNNING
    @Volatile private var currentBytes: Long = 0L
    @Volatile private var currentTotal: Long = 0L
    @Volatile private var currentReason: Int = -1
    @Volatile private var cancelRequested: Boolean = false

    fun apkFile(context: Context): File =
        File(context.getExternalFilesDir(null), APK_FILE_NAME)

    fun pendingDownloadId(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(PREF_DOWNLOAD_ID, -1L)

    fun clearPendingDownloadId(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(PREF_DOWNLOAD_ID).apply()
    }

    fun enqueue(context: Context, url: String): Long {
        val id = System.currentTimeMillis()
        currentId = id
        currentStatus = DownloadManager.STATUS_RUNNING
        currentBytes = 0L
        currentTotal = 0L
        currentReason = -1
        cancelRequested = false

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(PREF_DOWNLOAD_ID, id).apply()

        val file = apkFile(context)
        if (file.exists()) file.delete()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadTo(url, file)
                if (currentId == id) {
                    currentStatus = if (cancelRequested) DownloadManager.STATUS_FAILED
                                     else DownloadManager.STATUS_SUCCESSFUL
                }
            } catch (e: Exception) {
                Log.d("TWKT", "AppUpdateDownloader: download failed ${e.javaClass.simpleName}: ${e.message}")
                if (currentId == id) {
                    currentStatus = DownloadManager.STATUS_FAILED
                    currentReason = (e as? HttpDownloadException)?.httpCode ?: -1
                }
                file.delete()
            }
        }
        return id
    }

    /**
     * Suit les redirections manuellement (Location header) -- diagnostic
     * complémentaire, cf. commentaire de classe. Cause RÉELLE du 404 trouvée
     * le 31/07/2026 (box mediouni) : le premier GET vers
     * github.com/.../releases/download/... échouait déjà en 404, AVANT toute
     * redirection -- donc pas un problème de suivi de redirection, mais
     * l'absence d'en-tête User-Agent (HttpURLConnection n'en envoie aucun par
     * défaut). fetchRemoteVersion() (qui envoie "User-Agent: Tawkit-Android")
     * et curl (qui envoie son propre UA) réussissaient tous les deux sur la
     * même URL -- GitHub/son CDN semble filtrer les requêtes sans UA
     * reconnaissable sur ce point d'accès précis. Le suivi manuel des
     * redirections est conservé (plus robuste, meilleure visibilité), même si
     * ce n'était pas la cause du problème.
     */
    private fun downloadTo(url: String, dest: File) {
        var currentUrl = url
        var redirectCount = 0
        while (true) {
            val connection = URL(currentUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            // 60s (au lieu de 20s) : constaté en conditions réelles (31/07/2026,
            // box mediouni) que la connexion réelle de cette box stalle parfois
            // plus de 20s en cours de transfert (débit faible/instable), ce qui
            // faisait échouer un téléchargement par ailleurs viable (redirection
            // + code HTTP 200 tous corrects, seule la lecture du corps timeout).
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Tawkit-Android")
            try {
                connection.connect()
                val code = connection.responseCode
                Log.d("TWKT", "AppUpdateDownloader: GET $currentUrl -> $code (${connection.responseMessage})")

                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) {
                        throw HttpDownloadException(code, "redirect sans Location")
                    }
                    if (++redirectCount > 5) {
                        throw HttpDownloadException(code, "trop de redirections")
                    }
                    currentUrl = location
                    continue
                }
                if (code !in 200..299) throw HttpDownloadException(code, "HTTP $code")

                currentTotal = connection.contentLengthLong
                connection.inputStream.use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            if (cancelRequested) throw java.io.IOException("cancelled")
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            total += read
                            currentBytes = total
                        }
                    }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Annule un téléchargement en cours (supprime aussi le fichier partiel). */
    fun cancel(context: Context, id: Long) {
        if (currentId == id) cancelRequested = true
        clearPendingDownloadId(context)
        val file = apkFile(context)
        if (file.exists()) file.delete()
    }

    /** Lit l'état courant d'un téléchargement (status + octets) sans bloquer longtemps. */
    fun queryProgress(context: Context, id: Long): DownloadProgress? {
        if (id != currentId) return null
        return DownloadProgress(currentStatus, currentBytes, currentTotal, currentReason)
    }

    /** Lance l'installateur de paquets Android sur l'APK déjà téléchargé. */
    fun installApk(context: Context) {
        val apkFile = apkFile(context)
        if (!apkFile.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }
}
