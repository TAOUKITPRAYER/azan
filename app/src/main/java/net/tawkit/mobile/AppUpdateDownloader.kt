package net.tawkit.mobile

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Télécharge l'APK de mise à jour via le DownloadManager natif d'Android au
 * lieu de déléguer à Chrome (ACTION_VIEW) : évite les téléchargements
 * Chrome bloqués juste avant la fin (connexion coupée silencieusement sur
 * le CDN GitHub) et permet de déclencher automatiquement l'installation une
 * fois le téléchargement réellement terminé (cf. UpdateDownloadReceiver).
 *
 * La progression est suivie et affichée par UpdateProgressDialog (barre de
 * progression + invitation à installer, en avant-plan dans l'app) plutôt que
 * via la notification système du DownloadManager — inaccessible ou invisible
 * sur la plupart des boîtiers TV Android (pas de tiroir de notifications
 * navigable à la télécommande).
 */
object AppUpdateDownloader {

    private const val PREFS_NAME = "tawkit_update_prefs"
    private const val PREF_DOWNLOAD_ID = "pending_download_id"
    private const val APK_FILE_NAME = "taoukit_update.apk"

    data class DownloadProgress(val status: Int, val bytesDownloaded: Long, val totalBytes: Long)

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
        val file = apkFile(context)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Mise à jour Tawkit")
            .setDescription("Téléchargement de la nouvelle version…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, APK_FILE_NAME)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = downloadManager.enqueue(request)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(PREF_DOWNLOAD_ID, id).apply()

        return id
    }

    /** Annule un téléchargement en cours (supprime aussi le fichier partiel). */
    fun cancel(context: Context, id: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(id)
        clearPendingDownloadId(context)
    }

    /** Lit l'état courant d'un téléchargement (status + octets) sans bloquer longtemps. */
    fun queryProgress(context: Context, id: Long): DownloadProgress? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesIdx  = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIdx  = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            if (statusIdx < 0) return null
            return DownloadProgress(
                status = it.getInt(statusIdx),
                bytesDownloaded = if (bytesIdx >= 0) it.getLong(bytesIdx) else 0L,
                totalBytes = if (totalIdx >= 0) it.getLong(totalIdx) else 0L
            )
        }
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
