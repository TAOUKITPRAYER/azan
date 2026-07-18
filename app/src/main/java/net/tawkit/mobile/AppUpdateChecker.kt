package net.tawkit.mobile

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * Vérifie la release GitHub publique portant le tag fixe "taoukit".
 *
 * Format recommandé dans la description de la release :
 *   versionCode: 2
 *   versionName: 10B
 *
 * Le versionCode doit aussi être incrémenté dans app/build.gradle pour chaque
 * APK. L'ancienne description "V10A_beta" reste comprise comme versionName.
 */
object AppUpdateChecker {

    private const val RELEASE_API_URL =
        "https://api.github.com/repos/TAOUKITPRAYER/taoukit/releases/tags/taoukit"
    private const val FALLBACK_APK_URL =
        "https://github.com/TAOUKITPRAYER/taoukit/releases/download/taoukit/taoukit.apk"
    private const val APK_ASSET_NAME = "taoukit.apk"

    private const val PREFS_NAME = "tawkit_update_check_prefs"
    private const val KEY_LAST_CHECK_DATE = "last_check_date"
    private const val AUTO_CHECK_HOUR = 10

    /**
     * Vérification automatique économe en data : au plus une fois par jour,
     * jamais avant 10h (heure locale de l'appareil). Si l'app est ouverte
     * plusieurs fois le même jour après 10h, seule la première déclenche un
     * appel réseau ; les suivantes sont ignorées silencieusement.
     */
    fun maybeAutoCheck(activity: AppCompatActivity) {
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.HOUR_OF_DAY) < AUTO_CHECK_HOUR) return

        val today = todayDateKey(calendar)
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_CHECK_DATE, null) == today) return

        prefs.edit().putString(KEY_LAST_CHECK_DATE, today).apply()
        check(activity, manual = false)
    }

    private fun todayDateKey(calendar: Calendar): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    private val versionCodePattern =
        Regex("""(?im)^\s*versionCode\s*[:=]\s*(\d+)\s*$""")
    private val versionNamePattern =
        Regex("""(?im)^\s*versionName\s*[:=]\s*(.+?)\s*$""")

    private data class RemoteVersion(
        val versionCode: Long?,
        val versionName: String,
        val downloadUrl: String
    )

    fun check(activity: AppCompatActivity, manual: Boolean) {
        if (!manual) {
            // Contrôle automatique silencieux : aucune boîte tant qu'on n'a
            // pas de résultat (et aucune si la vérification échoue).
            CoroutineScope(Dispatchers.IO).launch {
                val result = runCatching { fetchRemoteVersion() }
                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                    result.fold(
                        onSuccess = { remote -> showResult(activity, remote, manual = false) },
                        onFailure = { error -> Log.e("TWKT", "Update check failed", error) }
                    )
                }
            }
            return
        }

        checkManual(activity)
    }

    /**
     * Contrôle manuel (lien du menu) : une boîte s'affiche immédiatement avec
     * un cercle de chargement + bouton Annuler, puis se transforme en place
     * pour afficher le résultat (à jour / nouvelle version / échec) — pas de
     * deuxième boîte, l'utilisateur garde le même repère visuel du début à
     * la fin.
     */
    private fun checkManual(activity: AppCompatActivity) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(8))
        }
        val progressBar = ProgressBar(activity)
        val statusText = TextView(activity).apply {
            text = "Vérification de la nouvelle version…"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        container.addView(progressBar)
        container.addView(statusText)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Vérification des mises à jour")
            .setView(container)
            .setCancelable(false)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("OK", null)   // slot réservé, rempli si besoin ci-dessous
            .show()

        val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        btnPositive.visibility = View.GONE

        var cancelled = false
        btnNegative.setOnClickListener {
            cancelled = true
            dialog.dismiss()
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { fetchRemoteVersion() }
            activity.runOnUiThread {
                if (cancelled || activity.isFinishing || activity.isDestroyed) return@runOnUiThread

                progressBar.visibility = View.GONE
                dialog.setCancelable(true)

                result.fold(
                    onSuccess = { remote ->
                        fillManualResult(activity, dialog, statusText, btnPositive, btnNegative, remote)
                    },
                    onFailure = { error ->
                        Log.e("TWKT", "Update check failed", error)
                        dialog.setTitle("Vérification impossible")
                        statusText.text = "Impossible de vérifier la nouvelle version. " +
                            "Vérifiez la connexion Internet puis réessayez."
                        btnNegative.text = "Fermer"
                        btnNegative.setOnClickListener { dialog.dismiss() }
                        btnPositive.visibility = View.VISIBLE
                        btnPositive.text = "Réessayer"
                        btnPositive.setOnClickListener {
                            dialog.dismiss()
                            checkManual(activity)
                        }
                    }
                )
            }
        }
    }

    private fun fillManualResult(
        activity: AppCompatActivity,
        dialog: AlertDialog,
        statusText: TextView,
        btnPositive: android.widget.Button,
        btnNegative: android.widget.Button,
        remote: RemoteVersion
    ) {
        val (localCode, localName) = installedVersion(activity)
        val updateAvailable = remote.versionCode?.let { it > localCode }
            ?: (normalizeVersion(remote.versionName) != normalizeVersion(localName))

        if (!updateAvailable) {
            dialog.setTitle("Application à jour")
            statusText.text = "Vous disposez de la dernière version disponible ($localName)."
            btnNegative.visibility = View.GONE
            btnPositive.visibility = View.VISIBLE
            btnPositive.text = "OK"
            btnPositive.setOnClickListener { dialog.dismiss() }
            return
        }

        dialog.setTitle("Nouvelle version disponible")
        statusText.text = "Version installée : $localName\n" +
            "Nouvelle version : ${remote.versionName}\n\n" +
            "Voulez-vous la télécharger et lancer la mise à jour ?"
        btnNegative.text = "Plus tard"
        btnNegative.setOnClickListener { dialog.dismiss() }
        btnPositive.visibility = View.VISIBLE
        btnPositive.text = "Mettre à jour"
        btnPositive.setOnClickListener {
            dialog.dismiss()
            openDownload(activity, remote.downloadUrl)
        }
    }

    private fun fetchRemoteVersion(): RemoteVersion {
        val connection = (URL(RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Tawkit-Android")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("GitHub HTTP $status")
            val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val release = JSONObject(json)
            val body = release.optString("body", "").trim()

            val remoteCode = versionCodePattern.find(body)?.groupValues?.get(1)?.toLongOrNull()
            val explicitName = versionNamePattern.find(body)?.groupValues?.get(1)?.trim()
            // Compatibilité avec la release actuelle dont le body est "V10A_beta".
            val legacyName = body.lineSequence().map { it.trim() }
                .firstOrNull { it.isNotEmpty() && !it.startsWith("versionCode", true) }
            val remoteName = explicitName ?: legacyName
                ?: throw IllegalStateException("Version absente de la description GitHub")

            var apkUrl = FALLBACK_APK_URL
            val assets = release.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    if (asset.optString("name") == APK_ASSET_NAME) {
                        apkUrl = asset.optString("browser_download_url", FALLBACK_APK_URL)
                        break
                    }
                }
            }
            return RemoteVersion(remoteCode, remoteName, apkUrl)
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun installedVersion(activity: AppCompatActivity): Pair<Long, String> {
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
        return code to (info.versionName ?: code.toString())
    }

    /** Utilisé uniquement par le contrôle automatique silencieux (aucune boîte si déjà à jour). */
    private fun showResult(activity: AppCompatActivity, remote: RemoteVersion, manual: Boolean) {
        val (localCode, localName) = installedVersion(activity)
        val updateAvailable = remote.versionCode?.let { it > localCode }
            ?: (normalizeVersion(remote.versionName) != normalizeVersion(localName))

        if (!updateAvailable) return

        AlertDialog.Builder(activity)
            .setTitle("Nouvelle version disponible")
            .setMessage(
                "Version installée : $localName\n" +
                    "Nouvelle version : ${remote.versionName}\n\n" +
                    "Voulez-vous la télécharger et lancer la mise à jour ?"
            )
            .setNegativeButton("Plus tard", null)
            .setPositiveButton("Mettre à jour") { _, _ ->
                openDownload(activity, remote.downloadUrl)
            }
            .show()
    }

    private fun normalizeVersion(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .removePrefix("v")
            .removeSuffix("beta")

    private fun openDownload(activity: AppCompatActivity, url: String) {
        try {
            UpdateProgressDialog.start(activity, url)
        } catch (error: Exception) {
            Log.e("TWKT", "Update download failed to start", error)
            AlertDialog.Builder(activity)
                .setTitle("Téléchargement impossible")
                .setMessage("Le téléchargement de la mise à jour n'a pas pu démarrer.")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}