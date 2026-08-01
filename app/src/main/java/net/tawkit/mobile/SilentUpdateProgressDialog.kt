package net.tawkit.mobile

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Affiche sur l'écran de la box la progression d'une mise à jour silencieuse
 * (déclenchée par push ou par le sondage périodique, cf. RemoteSilentUpdater)
 * -- demandé explicitement le 31/07/2026 : contrairement à UpdateProgressDialog
 * (flux manuel, "Vérifier les mises à jour"), ici personne n'appuie sur rien
 * -- ce dialogue est purement informatif, sans bouton, et se ferme tout seul
 * quelques secondes après le résultat final.
 *
 * Un seul dialogue actif à la fois (comme RemoteSilentUpdater/AppUpdateDownloader,
 * qui ne gèrent eux aussi qu'un seul téléchargement en vol).
 */
object SilentUpdateProgressDialog {

    private const val AUTO_DISMISS_DELAY_MS = 6000L

    private var dialog: AlertDialog? = null
    private var statusText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var percentText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    fun update(activity: AppCompatActivity, progress: RemoteSilentUpdater.Progress) {
        if (activity.isFinishing || activity.isDestroyed) return
        handler.removeCallbacksAndMessages(null)

        if (dialog == null) {
            buildDialog(activity)
        }

        when (progress.phase) {
            "checking" -> {
                progressBar?.isIndeterminate = true
                percentText?.text = ""
                statusText?.text = progress.message
            }
            "downloading" -> {
                progressBar?.isIndeterminate = false
                progressBar?.progress = progress.pct ?: 0
                percentText?.text = if (progress.totalBytes > 0)
                    "${progress.pct ?: 0} %  (${formatMo(progress.bytesDownloaded)} / ${formatMo(progress.totalBytes)})"
                else ""
                statusText?.text = progress.message
            }
            "installing" -> {
                progressBar?.isIndeterminate = true
                percentText?.text = ""
                statusText?.text = progress.message
            }
            "success", "failed" -> {
                progressBar?.isIndeterminate = false
                progressBar?.progress = if (progress.phase == "success") 100 else 0
                percentText?.text = ""
                statusText?.text = progress.message
                handler.postDelayed({ dismiss() }, AUTO_DISMISS_DELAY_MS)
            }
        }
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        try { dialog?.dismiss() } catch (e: Exception) { /* déjà fermé/Activity partie */ }
        dialog = null
        statusText = null
        progressBar = null
        percentText = null
    }

    private fun buildDialog(activity: AppCompatActivity) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(8))
        }
        val status = TextView(activity).apply {
            text = "Mise à jour en cours…"
            textSize = 15f
        }
        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            max = 100
        }
        val percent = TextView(activity).apply {
            gravity = Gravity.END
            textSize = 13f
            alpha = 0.7f
        }
        container.addView(status)
        container.addView(
            bar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(16); bottomMargin = dp(6) }
        )
        container.addView(percent)

        dialog = AlertDialog.Builder(activity)
            .setTitle("Mise à jour Tawkit")
            .setView(container)
            .setCancelable(false)
            .show()
        statusText = status
        progressBar = bar
        percentText = percent
    }

    private fun formatMo(bytes: Long): String =
        String.format("%.1f Mo", bytes / (1024.0 * 1024.0))
}
