package net.tawkit.mobile

import android.app.AlertDialog
import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Affiche la progression du téléchargement de la mise à jour directement
 * dans l'application (barre de progression + invitation à installer en fin
 * de téléchargement), au lieu de compter sur la notification système du
 * DownloadManager — invisible ou inaccessible sur la plupart des boîtiers TV
 * Android (pas de tiroir de notifications navigable à la télécommande).
 * Tout le cycle (démarrage, suivi, échec/succès, installation) reste piloté
 * depuis cette boîte de dialogue, sans jamais renvoyer l'utilisateur vers un
 * écran système externe avant l'étape finale d'installation (qui, elle,
 * appartient nécessairement au système — cf. REQUEST_INSTALL_PACKAGES).
 */
object UpdateProgressDialog {

    private const val POLL_INTERVAL_MS = 400L

    fun start(activity: AppCompatActivity, url: String) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(8))
        }
        val statusText = TextView(activity).apply {
            text = "Préparation du téléchargement…"
            textSize = 15f
        }
        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            max = 100
        }
        val percentText = TextView(activity).apply {
            gravity = Gravity.END
            textSize = 13f
            alpha = 0.7f
        }
        container.addView(statusText)
        container.addView(
            progressBar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(16); bottomMargin = dp(6) }
        )
        container.addView(percentText)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Mise à jour Tawkit")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Installer", null)   // slots réservés, écouteurs branchés
            .setNegativeButton("Annuler", null)      // manuellement ci-dessous
            .show()

        val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        btnPositive.visibility = View.GONE

        val handler = Handler(Looper.getMainLooper())
        var downloadId = AppUpdateDownloader.enqueue(activity, url)
        var finished = false
        var pollRunnable: Runnable? = null

        fun stopPolling() { pollRunnable?.let { handler.removeCallbacks(it) } }

        btnNegative.setOnClickListener {
            stopPolling()
            if (!finished) AppUpdateDownloader.cancel(activity, downloadId)
            dialog.dismiss()
        }

        fun showFailure() {
            finished = true
            statusText.text = "Échec du téléchargement"
            progressBar.isIndeterminate = false
            progressBar.progress = 0
            dialog.setCancelable(true)
            btnNegative.text = "Fermer"
            btnNegative.setOnClickListener { dialog.dismiss() }
            btnPositive.visibility = View.VISIBLE
            btnPositive.text = "Réessayer"
            btnPositive.setOnClickListener {
                dialog.dismiss()
                start(activity, url)
            }
        }

        fun showSuccess() {
            finished = true
            // Coordination avec UpdateDownloadReceiver (cf. son commentaire) : si
            // pendingDownloadId ne correspond plus, le receiver a déjà tout géré
            // (app passée en arrière-plan pendant le téléchargement) -> ne pas
            // ré-proposer l'installation en double, juste fermer silencieusement.
            val claimedHere = AppUpdateDownloader.pendingDownloadId(activity) == downloadId
            if (claimedHere) AppUpdateDownloader.clearPendingDownloadId(activity)

            progressBar.isIndeterminate = false
            progressBar.progress = 100
            percentText.text = "100 %"
            dialog.setCancelable(true)
            btnNegative.text = "Plus tard"
            btnNegative.setOnClickListener { dialog.dismiss() }

            if (!claimedHere) {
                statusText.text = "Téléchargement terminé (installation déjà lancée)"
                return
            }
            statusText.text = "Téléchargement terminé"
            btnPositive.visibility = View.VISIBLE
            btnPositive.text = "Installer"
            btnPositive.setOnClickListener {
                AppUpdateDownloader.installApk(activity)
                dialog.dismiss()
            }
        }

        pollRunnable = object : Runnable {
            override fun run() {
                if (finished) return
                if (activity.isFinishing || activity.isDestroyed) { stopPolling(); return }

                val progress = AppUpdateDownloader.queryProgress(activity, downloadId)
                if (progress == null) { handler.postDelayed(this, POLL_INTERVAL_MS); return }

                when (progress.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> showSuccess()
                    DownloadManager.STATUS_FAILED -> showFailure()
                    else -> {
                        if (progress.totalBytes > 0) {
                            progressBar.isIndeterminate = false
                            val pct = (progress.bytesDownloaded * 100 / progress.totalBytes).toInt()
                            progressBar.progress = pct
                            percentText.text = "$pct %  (${formatMo(progress.bytesDownloaded)} / ${formatMo(progress.totalBytes)})"
                        }
                        statusText.text = "Téléchargement en cours…"
                        handler.postDelayed(this, POLL_INTERVAL_MS)
                    }
                }
            }
        }
        handler.post(pollRunnable)
    }

    private fun formatMo(bytes: Long): String =
        String.format("%.1f Mo", bytes / (1024.0 * 1024.0))
}
