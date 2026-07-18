package net.tawkit.mobile

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Reçoit ACTION_DOWNLOAD_COMPLETE une fois l'APK de mise à jour réellement
 * téléchargé par DownloadManager (cf. AppUpdateDownloader.enqueue) et lance
 * directement l'installation — filet de sécurité pour le cas où l'app a été
 * fermée/mise en arrière-plan pendant le téléchargement (sans quoi
 * UpdateProgressDialog, qui gère normalement toute la fin du flux en
 * avant-plan, ne serait plus là pour le faire).
 *
 * Coordination avec UpdateProgressDialog : les deux consultent le même
 * pendingDownloadId et le vident dès qu'ils traitent un succès — le premier
 * des deux à s'exécuter "gagne" (Handler et BroadcastReceiver tournent tous
 * deux sur le thread principal, donc pas de vraie concurrence), l'autre voit
 * un id déjà vidé et ne fait rien, évitant un double déclenchement de
 * l'installateur.
 */
class UpdateDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (completedId == -1L || completedId != AppUpdateDownloader.pendingDownloadId(context)) return
        AppUpdateDownloader.clearPendingDownloadId(context)

        val progress = AppUpdateDownloader.queryProgress(context, completedId)
        if (progress?.status != DownloadManager.STATUS_SUCCESSFUL) {
            Log.e("TWKT", "UpdateDownloadReceiver: download failed or was cancelled")
            return
        }

        AppUpdateDownloader.installApk(context)
    }
}
