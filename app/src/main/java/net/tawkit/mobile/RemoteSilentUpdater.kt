package net.tawkit.mobile

import android.app.DownloadManager
import android.content.Context
import kotlinx.coroutines.delay

/**
 * Mise à jour déclenchée à distance (cf. remote_action 'update_app',
 * custom.js listener ucRemoteAction, garde box-only déjà en place) : vérifie
 * la dernière release GitHub, la télécharge, puis tente l'installation
 * silencieuse en 2 temps -- DeviceOwnerInstaller (API officielle
 * PackageInstaller, actif seulement sur les box provisionnées Device Owner,
 * cf. TawkitDeviceAdminReceiver) en priorité, puis SilentUpdateHelper (su,
 * marche rarement -- cf. son commentaire de classe) en second essai. Si
 * aucun des deux ne fonctionne, se rabat sur l'installateur système standard
 * (AppUpdateDownloader.installApk, 1 tap requis sur place) plutôt que
 * d'échouer silencieusement.
 */
object RemoteSilentUpdater {

    data class Outcome(val success: Boolean, val message: String, val silent: Boolean)

    /** phase: "checking" | "downloading" | "installing" | "success" | "failed". */
    data class Progress(
        val phase: String,
        val message: String,
        val pct: Int? = null,
        val bytesDownloaded: Long = 0,
        val totalBytes: Long = 0
    )

    // APK ~96 Mo : budget large avant d'abandonner le polling. Porté de 120s
    // -> 600s le 31/07/2026 (box mediouni), puis -> 3600s le 08/09/2026 :
    // depuis que AppUpdateDownloader reprend via Range (partiel conservé,
    // retry/backoff internes), un téléchargement sur lien très faible (KM22 :
    // Wi-Fi mosquée + Tailscale DERP ~15-35 Ko/s -> ~45-90 min pour 96 Mo)
    // peut légitimement aboutir bien au-delà de 10 min. On borne quand même
    // (1 h) et on abandonne plus tôt si AUCUN octet ne progresse pendant
    // STALL_GIVEUP_SECONDS (lien réellement mort -- en principe
    // AppUpdateDownloader passe STATUS_FAILED avant, ceci n'est qu'un filet).
    private const val MAX_POLL_SECONDS = 3600
    private const val STALL_GIVEUP_SECONDS = 300

    suspend fun run(context: Context, onProgress: ((Progress) -> Unit)? = null): Outcome {
        // AUCUN onProgress avant d'avoir confirmé qu'une mise à jour existe
        // réellement -- le sondage périodique (toutes les 60s en test) appelle
        // run() en continu ; émettre "checking" ici écraserait le dernier
        // résultat réel (succès/échec) dans Supabase au bout d'une minute à
        // chaque fois (constaté 31/07/2026, corrigé le jour même).
        val remote = runCatching { AppUpdateChecker.fetchRemoteVersion() }.getOrNull()
            ?: return Outcome(false, "Vérification GitHub impossible (réseau ou release introuvable)", false)

        val (localCode, localName) = AppUpdateChecker.installedVersion(context)
        val newer = remote.versionCode?.let { it > localCode }
        if (newer != true) {
            // Pas d'event ici non plus : ce n'est pas un échec, juste rien à
            // faire -- pas de dialogue/rapport à afficher pour un cas aussi
            // fréquent.
            return Outcome(false, "Déjà à jour (installée: $localName, distante: ${remote.versionName})", false)
        }

        onProgress?.invoke(Progress("checking", "Nouvelle version détectée (${remote.versionName})…"))

        val downloadId = AppUpdateDownloader.enqueue(context, remote.downloadUrl)
        var terminal = false
        var ok = false
        var waited = 0
        var lastPct = -1
        // Capture le code DownloadManager.COLUMN_REASON du dernier statut connu --
        // sans ça, un échec ne dit jamais POURQUOI (HTTP, stockage, sécurité...),
        // cf. investigation 31/07/2026 (box mediouni) : DownloadManager.STATUS_FAILED
        // atteint en quelques secondes seulement, timeout 120s jamais approché.
        var lastReason = -1
        var lastStatus = -1
        var lastBytes = -1L
        var stalledSeconds = 0
        while (!terminal && waited < MAX_POLL_SECONDS) {
            delay(1000)
            waited++
            val progress = AppUpdateDownloader.queryProgress(context, downloadId) ?: continue
            lastStatus = progress.status
            lastReason = progress.reason
            when (progress.status) {
                DownloadManager.STATUS_SUCCESSFUL -> { terminal = true; ok = true }
                DownloadManager.STATUS_FAILED     -> terminal = true
                else -> {
                    // Abandon anticipé si le téléchargement ne progresse plus du
                    // tout (le retry/Range d'AppUpdateDownloader devrait déjà
                    // avoir rendu STATUS_FAILED avant, ceci n'est qu'un filet).
                    if (progress.bytesDownloaded > lastBytes) {
                        lastBytes = progress.bytesDownloaded
                        stalledSeconds = 0
                    } else if (++stalledSeconds >= STALL_GIVEUP_SECONDS) {
                        terminal = true
                    }
                    // Throttle à chaque palier de 1% -- évite d'inonder le dialogue
                    // natif et le rapport Supabase (appelé jusqu'à 1x/s sinon).
                    if (progress.totalBytes > 0) {
                        val pct = (progress.bytesDownloaded * 100 / progress.totalBytes).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            onProgress?.invoke(
                                Progress("downloading", "Téléchargement…", pct, progress.bytesDownloaded, progress.totalBytes)
                            )
                        }
                    }
                }
            }
        }
        AppUpdateDownloader.clearPendingDownloadId(context)
        if (!ok) {
            val outcome = Outcome(false, "Échec du téléchargement (${remote.versionName}) status=$lastStatus reason=$lastReason waited=${waited}s", false)
            onProgress?.invoke(Progress("failed", outcome.message))
            return outcome
        }

        onProgress?.invoke(Progress("installing", "Installation…"))

        val apkFile = AppUpdateDownloader.apkFile(context)
        if (DeviceOwnerInstaller.installSilently(context, apkFile)) {
            val outcome = Outcome(true, "Installée silencieusement (Device Owner, ${remote.versionName})", true)
            onProgress?.invoke(Progress("success", outcome.message))
            return outcome
        }
        if (SilentUpdateHelper.installSilently(apkFile)) {
            val outcome = Outcome(true, "Installée silencieusement (su, ${remote.versionName})", true)
            onProgress?.invoke(Progress("success", outcome.message))
            return outcome
        }

        // Filet de sécurité : ni Device Owner ni root disponibles --
        // au moins amener l'utilisateur au dernier tap plutôt que de laisser
        // l'APK téléchargé sans suite (cf. discussion : jamais d'échec
        // silencieux, toujours un état observable).
        return try {
            AppUpdateDownloader.installApk(context)
            val outcome = Outcome(true, "Téléchargée (${remote.versionName}) — confirmation d'installation requise sur la box", false)
            onProgress?.invoke(Progress("success", outcome.message))
            outcome
        } catch (e: Exception) {
            val outcome = Outcome(false, "Téléchargée mais impossible d'ouvrir l'installateur : ${e.message}", false)
            onProgress?.invoke(Progress("failed", outcome.message))
            outcome
        }
    }
}
