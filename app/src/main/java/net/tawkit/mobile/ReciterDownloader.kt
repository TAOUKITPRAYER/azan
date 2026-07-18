package net.tawkit.mobile

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Telecharge un recitateur (archive ZIP contenant meta.json + NNN.ogg) en
 * arriere-plan via WorkManager, avec reprise (Range bytes=) si le fichier
 * .part existe deja. A la fin, decompresse dans reciters/<id>/ et nettoie.
 *
 * Pause  = WorkManager.cancelUniqueWork()  -> le .part est conserve sur disque
 * Resume = ré-enqueue le meme travail      -> repart de la taille du .part
 * Cancel = cancel + suppression du .part et du fichier de progression
 *
 * Progression lue par JS via MobileJsBridge.getDownloadProgress(id), qui lit
 * le fichier JSON "<id>.progress.json" ecrit par ce worker (~2x/seconde).
 */
class ReciterDownloadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()

        val baseDir = ReciterManager.getBaseDir(applicationContext)
        val partFile = File(baseDir, "$id.zip.part")
        val progressFile = File(baseDir, "$id.progress.json")

        // Hissees hors du try (au lieu de val locaux) pour que le catch generique
        // puisse rapporter une progression/total exacts (et pas total=-1 alors
        // qu'on le connaissait) — utile pour le bouton "reprendre" cote JS, qui
        // affiche le pourcentage atteint au moment de l'erreur.
        var downloaded = if (partFile.exists()) partFile.length() else 0L
        var totalBytes = -1L
        var conn: HttpURLConnection? = null
        try {
            val startOffset = downloaded
            writeProgress(progressFile, "downloading", startOffset, -1)

            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                // 30s de lecture (au lieu de 15s) : un transfert de 200+ Mo sur un
                // reseau mobile peut avoir des creux de quelques secondes sans
                // etre mort pour autant — 15s declenchait des "timeout" abusifs
                // en cours de telechargement (cf. log du 27/06, ~89% atteint).
                connectTimeout = 20000
                readTimeout = 30000
                if (startOffset > 0) setRequestProperty("Range", "bytes=$startOffset-")
                connect()
            }

            val code = conn.responseCode

            // HTTP 416 (Range Not Satisfiable) : le cas le plus frequent n'est PAS
            // un fichier corrompu mais un .part DEJA complet dont le worker n'a
            // jamais pu finaliser l'extraction (ex. processus tue par l'OS ou par
            // l'installation d'une mise a jour de l'app pendant le telechargement,
            // cf. retour utilisateur du 2026-07-14). Sans ce traitement, reprendre
            // redemande toujours "les octets apres la fin du fichier" -> 416 en
            // boucle, blocage permanent. Le serveur renvoie generalement l'en-tete
            // Content-Range: bytes */<taille reelle> meme sur une reponse 416,
            // ce qui permet de verifier si le .part est deja complet et, si oui,
            // de sauter directement a l'extraction au lieu d'echouer.
            if (code == 416) {   // HTTP_REQUESTED_RANGE_NOT_SATISFIABLE (absent de java.net.HttpURLConnection)
                val remoteTotal = conn.getHeaderField("Content-Range")
                    ?.substringAfterLast('/')?.toLongOrNull()
                if (remoteTotal != null && downloaded >= remoteTotal) {
                    conn.disconnect()
                    writeProgress(progressFile, "extracting", downloaded, remoteTotal)
                    setForeground(buildForegroundInfo(id, "extraction", 100))
                    return finalizeDownload(id, baseDir, partFile, progressFile, downloaded, remoteTotal)
                }
                // Taille reelle inconnue ou .part visiblement incoherent (plus
                // petit que prevu mais range quand meme refusee) : etat non
                // recuperable en l'etat -> on repart de zero plutot que de rester
                // bloque indefiniment sur la meme erreur.
                partFile.delete()
                writeProgress(progressFile, "retrying", 0, -1,
                    "Reprise impossible (HTTP 416) — nouveau téléchargement complet")
                return Result.retry()
            }

            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                // 5xx = probleme cote serveur/CDN, generalement transitoire ->
                // on retente automatiquement (WorkManager backoff) plutot que
                // d'exiger un clic utilisateur. 4xx (404, etc.) ne changera pas
                // au prochain essai -> echec immediat.
                if (code >= 500 && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    writeProgress(progressFile, "retrying", downloaded, -1,
                        "Erreur serveur HTTP $code — nouvelle tentative automatique (${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                    return Result.retry()
                }
                writeProgress(progressFile, "error", partFile.length(), -1, "HTTP $code")
                return Result.failure()
            }
            val resuming = (code == HttpURLConnection.HTTP_PARTIAL)
            val already = if (resuming) startOffset else 0L
            downloaded = already
            val contentLength = conn.contentLengthLong
            totalBytes = if (contentLength > 0) already + contentLength else -1L

            // Foreground service : promeut le worker dès maintenant pour qu'il
            // survive si l'utilisateur change d'appli (écran allumé mais appli
            // en arrière-plan) — setKeepScreenOn() côté JS ne couvre que le cas
            // "écran éteint, appli au premier plan". Les deux combinés couvrent
            // l'ensemble des cas signalés.
            setForeground(buildForegroundInfo(id, "telechargement", percentOf(already, totalBytes)))

            // DIAGNOSTIC : photo des en-têtes au moment de la connexion — utile
            // pour distinguer un CDN qui ignore la reprise (Range), qui (de)
            // compresse en vol (Content-Encoding), ou qui ne renvoie pas de
            // Content-Length (totalBytes=-1, donc pas de validation de taille
            // possible plus bas).
            writeProgress(progressFile, "downloading", already, totalBytes, "", mapOf(
                "httpCode" to code.toString(),
                "resuming" to resuming.toString(),
                "startOffset" to startOffset.toString(),
                "contentLength" to contentLength.toString(),
                "contentEncoding" to (conn.contentEncoding ?: "")
            ))

            // (downloaded est deja initialise a "already" ci-dessus, en var de
            // portee fonction — pas de redeclaration locale ici : le catch
            // generique doit voir la meme variable mutee par la boucle.)
            FileOutputStream(partFile, resuming).use { out ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var lastWrite = 0L
                    var lastNotif = 0L
                    while (true) {
                        if (isStopped) {
                            // DIAGNOSTIC : isStopped=true ici veut dire que WorkManager a
                            // annule le worker pendant la boucle de lecture — soit par
                            // pause() explicite (utilisateur), soit par l'OS (Doze /
                            // app en arriere-plan / ecran eteint). Le message permet de
                            // distinguer ce cas dans les logs ; pause() ecrase ensuite ce
                            // fichier avec son propre message si c'est bien l'utilisateur.
                            writeProgress(progressFile, "paused", downloaded, totalBytes,
                                "arrêt worker (isStopped=true) — cause probable : pause manuelle OU OS (Doze/écran éteint/app en arrière-plan)")
                            return Result.failure()
                        }
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        downloaded += n
                        val now = System.currentTimeMillis()
                        if (now - lastWrite > 400) {
                            writeProgress(progressFile, "downloading", downloaded, totalBytes)
                            lastWrite = now
                        }
                        // Notification rafraichie moins souvent que le fichier de
                        // progression (appel binder vers NotificationManager, pas
                        // besoin de la frequence de writeProgress).
                        if (now - lastNotif > 2000) {
                            setForeground(buildForegroundInfo(id, "telechargement", percentOf(downloaded, totalBytes)))
                            lastNotif = now
                        }
                    }
                }
            }
            writeProgress(progressFile, "extracting", downloaded, totalBytes)
            setForeground(buildForegroundInfo(id, "extraction", 100))

            // Validation : un flux tronqué (CDN/réseau capricieux) ne lève pas
            // toujours d'exception cote Java — sans ce check, un .zip incomplet
            // serait quand meme decompresse et installe "tel quel" (audio corrompu,
            // meta.json parfois absent -> nom affiche = id du dossier). On verifie
            // donc explicitement la taille recue avant de toucher au zip.
            if (totalBytes > 0 && downloaded != totalBytes) {
                // Flux tronqué côté serveur/CDN, souvent transitoire (comme un
                // timeout) -> meme logique de retry borné ; le .part conserve
                // les octets déjà reçus, la reprise repartira de "downloaded".
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    writeProgress(progressFile, "retrying", downloaded, totalBytes,
                        "Téléchargement incomplet ($downloaded/$totalBytes) — nouvelle tentative automatique (${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                    return Result.retry()
                }
                writeProgress(progressFile, "error", downloaded, totalBytes,
                    "Téléchargement incomplet : $downloaded/$totalBytes octets reçus — réessayez")
                return Result.failure()
            }

            return finalizeDownload(id, baseDir, partFile, progressFile, downloaded, totalBytes)
        } catch (e: Exception) {
            // Toute IOException (SocketTimeoutException, connexion reinitialisee,
            // hote injoignable...) est par nature transitoire sur mobile -> on
            // retente automatiquement (WorkManager backoff, cf. enqueue()) au lieu
            // d'exiger un clic utilisateur. Le .part n'est jamais supprime ici :
            // la prochaine tentative repart de "downloaded" via Range. On rapporte
            // le total connu (et plus -1) pour que le pourcentage reste correct
            // cote JS pendant l'attente.
            val onDisk = partFile.length()
            if (e is IOException && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                writeProgress(progressFile, "retrying", onDisk, totalBytes,
                    "Connexion interrompue (" + (e.message ?: "réseau") + ") — nouvelle tentative automatique (${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                return Result.retry()
            }
            writeProgress(progressFile, "error", onDisk, totalBytes,
                (e.message ?: "erreur reseau") + " — le téléchargement reprendra où il s'est arrêté")
            return Result.failure()
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Extrait le .part termine (taille validee par l'appelant) et marque le
     * telechargement "done". Partagee entre le chemin normal (fin de boucle
     * de lecture) et la recuperation HTTP 416 (.part deja complet mais jamais
     * finalise, cf. commentaire plus haut) pour ne pas dupliquer cette logique.
     */
    private fun finalizeDownload(
        id: String, baseDir: File, partFile: File, progressFile: File,
        downloaded: Long, totalBytes: Long
    ): Result {
        val extractResult: ExtractResult
        try {
            extractResult = extractZip(partFile, File(baseDir, id))
        } catch (e: Exception) {
            // Zip corrompu/tronque : ne pas laisser une installation a moitie
            // faite (qui apparaitrait comme "installee" mais illisible).
            File(baseDir, id).deleteRecursively()
            writeProgress(progressFile, "error", downloaded, totalBytes,
                "Archive corrompue : " + (e.message ?: "erreur d'extraction") + " — réessayez")
            return Result.failure()
        }
        partFile.delete()
        // DIAGNOSTIC : entryCount/hasMeta permettent de verifier, sans avoir
        // besoin d'aller fouiller sur le telephone, que l'archive contenait
        // bien les 114 pistes + meta.json attendus (cf. nom affiche = id au
        // lieu du nom localise -> hasMeta=false est la preuve directe).
        writeProgress(progressFile, "done",
            File(baseDir, id).walkTopDown().filter { it.isFile }.sumOf { it.length() }, totalBytes, "",
            mapOf("entryCount" to extractResult.entryCount.toString(), "hasMeta" to extractResult.hasMeta.toString()))
        return Result.success()
    }

    /** Resultat d'extraction : utilise uniquement a des fins de diagnostic (cf. writeProgress "done"). */
    private data class ExtractResult(val entryCount: Int, val hasMeta: Boolean)

    private fun extractZip(zipFile: File, targetDir: File): ExtractResult {
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()
        var entryCount = 0
        var hasMeta = false
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            val buffer = ByteArray(64 * 1024)
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // a plat : on ignore les sous-dossiers eventuels dans le zip
                    val flatName = File(entry.name).name
                    val outFile = File(targetDir, flatName)
                    FileOutputStream(outFile).use { fos ->
                        while (true) {
                            val len = zis.read(buffer)
                            if (len < 0) break
                            fos.write(buffer, 0, len)
                        }
                    }
                    entryCount++
                    if (flatName.equals("meta.json", ignoreCase = true)) hasMeta = true
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return ExtractResult(entryCount, hasMeta)
    }

    private fun percentOf(downloaded: Long, total: Long): Int =
        if (total > 0) (downloaded * 100 / total).toInt() else -1

    /**
     * Notification de progression + promotion en foreground service. Sans ça,
     * WorkManager exécute le worker comme une tâche d'arrière-plan classique :
     * l'OS peut la limiter/suspendre dès que l'appli n'est plus au premier
     * plan (en plus du cas écran éteint déjà couvert par setKeepScreenOn côté
     * JS). dataSync est le type adapté à un transfert de fichier en arrière-plan
     * (cf. AndroidManifest.xml : permissions + déclaration du type sur
     * androidx.work.impl.foreground.SystemForegroundService).
     */
    private fun buildForegroundInfo(id: String, phase: String, percent: Int): ForegroundInfo {
        val notifId = NOTIF_ID_BASE + (id.hashCode() and 0xFFFF)
        val text = when {
            phase == "extraction" -> "Installation de \"$id\"…"
            percent in 0..100     -> "Téléchargement de \"$id\" : $percent%"
            else                  -> "Téléchargement de \"$id\"…"
        }
        val notification = NotificationCompat.Builder(applicationContext, MobileJsBridge.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Récitateurs Coran")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, if (percent in 0..100) percent else 0, percent < 0)
            .build()
        return ForegroundInfo(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        private const val NOTIF_ID_BASE = 90000
        private const val KEY_ID = "id"
        private const val KEY_URL = "url"

        /** Nombre de tentatives auto (Result.retry()) avant abandon definitif sur
         *  erreur transitoire (timeout reseau, HTTP 5xx, flux tronque) — au-dela,
         *  on bascule en "error" terminal et l'utilisateur doit relancer lui-meme. */
        private const val MAX_RETRY_ATTEMPTS = 5

        private fun workName(id: String) = "reciter_download_$id"

        fun enqueue(context: Context, id: String, url: String) {
            val req = OneTimeWorkRequestBuilder<ReciterDownloadWorker>()
                .setInputData(workDataOf(KEY_ID to id, KEY_URL to url))
                // Pas la peine de tourner (et de retenter) sans connexion du tout ;
                // WorkManager attendra qu'un reseau soit disponible pour demarrer/
                // relancer le worker.
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                // Backoff lineaire (10s, 20s, 30s...) entre les tentatives Result.retry() —
                // evite de marteler un serveur/reseau qui vient de couper.
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, req)
        }

        /** Annule le worker en cours ; le fichier .part est conserve pour reprise ulterieure. */
        fun pause(context: Context, id: String) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(id))
            val baseDir = ReciterManager.getBaseDir(context)
            // Ecrase le message "arrêt worker (isStopped=true)..." du worker par un
            // message non-ambigu : si ce fichier finit avec CE message, on sait que
            // c'est l'utilisateur qui a appuye sur pause (et pas l'OS).
            writeProgress(File(baseDir, "$id.progress.json"), "paused", File(baseDir, "$id.zip.part").length(), -1,
                "arrêt manuel (utilisateur a appuyé sur pause)")
        }

        /** Annule et supprime toute trace (part + progression). */
        fun cancel(context: Context, id: String) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(id))
            val baseDir = ReciterManager.getBaseDir(context)
            File(baseDir, "$id.zip.part").delete()
            File(baseDir, "$id.progress.json").delete()
        }

        fun getProgress(context: Context, id: String): JSONObject {
            val f = File(ReciterManager.getBaseDir(context), "$id.progress.json")
            if (!f.exists()) return JSONObject().apply { put("status", "idle") }
            return try {
                JSONObject(f.readText())
            } catch (e: Exception) {
                JSONObject().apply { put("status", "idle") }
            }
        }

        private fun writeProgress(
            file: File, status: String, downloaded: Long, total: Long, message: String = "",
            extra: Map<String, String> = emptyMap()
        ) {
            try {
                val obj = JSONObject().apply {
                    put("status", status)
                    put("downloaded", downloaded)
                    put("total", total)
                    put("percent", if (total > 0) (downloaded * 100 / total) else -1)
                    if (message.isNotEmpty()) put("message", message)
                    // DIAGNOSTIC : champs additionnels (httpCode, contentEncoding,
                    // entryCount, hasMeta, etc.) lus et logges integralement cote JS
                    // (_rmPollDownload fait un JSON.stringify(p) complet a chaque
                    // transition de statut) — voir custom.js.
                    extra.forEach { (k, v) -> put(k, v) }
                }
                file.writeText(obj.toString())
            } catch (e: Exception) { /* best-effort */ }
        }
    }
}
