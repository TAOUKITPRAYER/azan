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
import java.io.FileOutputStream
import java.io.IOException
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
 * RÉELLE identifiée ensuite (voir commentaire de downloadAttempt ci-dessous) :
 * absence d'en-tête User-Agent sur la requête -- probablement filtrée par
 * GitHub/son CDN sur ce point d'accès précis, indépendamment de
 * DownloadManager ou d'HttpURLConnection. La bascule vers une implémentation
 * HTTP directe n'était donc pas strictement nécessaire pour corriger le 404
 * (ajouter le même en-tête à DownloadManager.Request aurait sans doute
 * suffi), mais est conservée : meilleure visibilité (code HTTP, reason
 * exacte) et aucune dépendance à un composant système tiers.
 *
 * REPRISE HTTP (Range) -- ajout 08/09/2026. Sur les box à lien faible
 * (Wi-Fi mosquée + Tailscale relayé DERP, ~15-35 Ko/s), l'APK ~96 Mo ne
 * passait jamais d'une traite : le moindre stall réseau tuait le
 * téléchargement, qui repartait de zéro à chaque tentative (constaté KM22
 * 07/09/2026, figé à 37 % pendant ~2 h). Désormais :
 *   - le fichier partiel est CONSERVÉ entre les tentatives et entre deux
 *     appels (enqueue le reprend s'il vient du même url) ;
 *   - chaque tentative redemande la suite via `Range: bytes=<taille>-` et
 *     ajoute au fichier (réponse 206) ; repli propre si le serveur renvoie
 *     200 (Range ignoré) ou 416 (partiel périmé) ;
 *   - retry + backoff internes : le compteur de tentatives « bloquées » se
 *     remet à zéro dès qu'au moins RESET_PROGRESS_BYTES octets de plus sont
 *     reçus -> un lien lent mais vivant finit toujours ; seul un lien
 *     réellement mort abandonne (MAX_STALLED_ATTEMPTS, ou ABSOLUTE_MAX_ATTEMPTS).
 *
 * Les constantes DownloadManager.STATUS_* sont réutilisées telles quelles
 * (ce sont de simples int) pour ne rien changer à l'interface publique --
 * RemoteSilentUpdater et UpdateProgressDialog continuent de fonctionner sans
 * modification : pendant un backoff le statut reste STATUS_RUNNING (les deux
 * pollers le traitent comme « en cours »).
 *
 * Un seul téléchargement à la fois (garde `currentId` : une coroutine
 * supplantée par un enqueue plus récent s'arrête d'elle-même).
 */
object AppUpdateDownloader {

    private const val PREFS_NAME = "tawkit_update_prefs"
    private const val PREF_DOWNLOAD_ID = "pending_download_id"
    private const val PREF_DOWNLOAD_URL = "pending_download_url"
    private const val PREF_DOWNLOAD_TOTAL = "pending_download_total"
    // ETag (ou, à défaut, Last-Modified) de la ressource au moment où le
    // partiel a été écrit -> envoyé en `If-Range` à la reprise : le serveur
    // renvoie 206 seulement si l'APK n'a pas changé, sinon 200 (contenu
    // complet) et on repart de zéro. Couvre le cas `gh release upload
    // --clobber` (même URL, octets différents) sans suivi de version côté app.
    private const val PREF_DOWNLOAD_VALIDATOR = "pending_download_validator"
    private const val APK_FILE_NAME = "taoukit_update.apk"

    // Reprise : nb de tentatives consécutives SANS progrès notable avant
    // d'abandonner (lien mort). Se remet à 0 dès qu'on gagne >= RESET_PROGRESS_BYTES.
    private const val MAX_STALLED_ATTEMPTS = 8
    // Garde-fou absolu : même en progressant, on ne boucle pas indéfiniment
    // (un vrai trickle pathologique de quelques octets/minute finirait par
    // épuiser ceci). RemoteSilentUpdater a en plus son propre plafond de temps.
    private const val ABSOLUTE_MAX_ATTEMPTS = 60
    private const val RESET_PROGRESS_BYTES = 64 * 1024L
    private const val RETRY_BASE_DELAY_MS = 2_000L
    private const val RETRY_MAX_DELAY_MS = 20_000L

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

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prevUrl = prefs.getString(PREF_DOWNLOAD_URL, null)
        val prevTotal = prefs.getLong(PREF_DOWNLOAD_TOTAL, -1L)
        prefs.edit()
            .putLong(PREF_DOWNLOAD_ID, id)
            .putString(PREF_DOWNLOAD_URL, url)
            .apply()

        val file = apkFile(context)

        // Reprise possible uniquement si le partiel vient du MÊME url (release
        // inchangée). URL différente = nouvelle version -> le partiel est
        // obsolète, on repart propre.
        val haveBytes = if (file.exists() && prevUrl == url) file.length() else 0L
        if (haveBytes == 0L) {
            if (file.exists()) file.delete()
            // URL différente (nouvelle version) ou pas de partiel : le
            // validateur mémorisé ne vaut plus rien.
            prefs.edit().remove(PREF_DOWNLOAD_VALIDATOR).remove(PREF_DOWNLOAD_TOTAL).apply()
        }

        // Déjà complet (partiel == taille totale connue d'un passage précédent) :
        // rien à télécharger, on signale succès tout de suite.
        if (haveBytes > 0L && prevTotal > 0L && haveBytes >= prevTotal && prevUrl == url) {
            Log.d("TWKT", "AppUpdateDownloader: APK déjà complet en cache ($haveBytes octets), pas de téléchargement")
            currentTotal = prevTotal
            currentBytes = haveBytes
            currentStatus = DownloadManager.STATUS_SUCCESSFUL
            return id
        }

        if (haveBytes > 0L) {
            currentBytes = haveBytes
            if (prevTotal > 0L) currentTotal = prevTotal
            Log.d("TWKT", "AppUpdateDownloader: reprise à $haveBytes octets (url inchangée)")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadWithResume(context, url, file, id)
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
                // NB : on ne supprime PAS le fichier partiel sur erreur réseau
                // -> le prochain enqueue (retry manuel, ou vérif quotidienne)
                // reprendra là où on s'est arrêté. Seules les erreurs
                // définitives (4xx hors 408/429, ou 416 confirmé périmé)
                // purgent le partiel, dans downloadWithResume.
            }
        }
        return id
    }

    /**
     * Boucle de tentatives avec reprise. Chaque itération appelle
     * downloadAttempt() qui redemande la suite via Range et écrit dans `dest`.
     * On abandonne seulement si le lien ne progresse plus du tout
     * (MAX_STALLED_ATTEMPTS tentatives sans gagner RESET_PROGRESS_BYTES) ou
     * après ABSOLUTE_MAX_ATTEMPTS itérations, ou sur erreur HTTP définitive.
     */
    private fun downloadWithResume(context: Context, url: String, dest: File, gen: Long) {
        var stalledAttempts = 0
        var attempts = 0
        var progressMark = dest.length()

        while (true) {
            if (cancelRequested) throw IOException("cancelled")
            if (currentId != gen) throw IOException("superseded by newer download")
            attempts++
            if (attempts > ABSOLUTE_MAX_ATTEMPTS) {
                throw IOException("abandon après $attempts tentatives (${dest.length()} octets)")
            }

            try {
                val complete = downloadAttempt(context, url, dest, gen)
                if (complete) {
                    val total = currentTotal
                    val len = dest.length()
                    when {
                        total > 0L && len > total -> {
                            // Fichier trop gros = corruption (partiel incohérent
                            // avec la ressource). On repart propre.
                            Log.d("TWKT", "AppUpdateDownloader: fichier trop grand $len/$total -> purge et reprise de zéro")
                            dest.delete()
                            currentBytes = 0L
                            progressMark = 0L
                            stalledAttempts = 0
                        }
                        total > 0L && len < total -> {
                            // Flux terminé mais fichier court : serveur qui coupe
                            // tôt -> on reboucle (Range reprendra).
                            Log.d("TWKT", "AppUpdateDownloader: flux fini mais fichier court $len/$total, reprise")
                        }
                        else -> {
                            if (total > 0L) {
                                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                                    .putLong(PREF_DOWNLOAD_TOTAL, total).apply()
                            }
                            Log.d("TWKT", "AppUpdateDownloader: téléchargement complet $len octets")
                            return
                        }
                    }
                }
            } catch (e: HttpDownloadException) {
                when {
                    e.httpCode == 416 -> {
                        // Range non satisfiable : soit le partiel est déjà
                        // complet, soit il est périmé. On vérifie la vraie
                        // taille avant de jeter quoi que ce soit.
                        val realTotal = probeTotal(url)
                        if (realTotal > 0L && dest.length() >= realTotal) {
                            currentTotal = realTotal
                            currentBytes = dest.length()
                            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                                .putLong(PREF_DOWNLOAD_TOTAL, realTotal).apply()
                            Log.d("TWKT", "AppUpdateDownloader: 416 mais fichier déjà complet ($realTotal octets)")
                            return
                        }
                        Log.d("TWKT", "AppUpdateDownloader: 416, partiel périmé (${dest.length()}/${realTotal}) -> reprise de zéro")
                        dest.delete()
                        currentBytes = 0L
                        progressMark = 0L
                        stalledAttempts = 0
                    }
                    e.httpCode in 400..499 && e.httpCode != 408 && e.httpCode != 429 -> {
                        // 404 / 403 / 410 ... : définitif, inutile de réessayer.
                        dest.delete()
                        throw e
                    }
                    // 5xx / 408 / 429 : transitoire, on garde le partiel et on reboucle.
                }
            } catch (e: IOException) {
                if (cancelRequested) throw e
                if (currentId != gen) throw e
                // timeout / reset / DNS ... : transitoire, partiel conservé.
            }

            val now = dest.length()
            when {
                // Le fichier a rétréci = un repli « reprise de zéro » a eu lieu
                // (200 sur Range, 416, corruption) -> on repart le comptage à neuf.
                now < progressMark -> { progressMark = now; stalledAttempts = 0 }
                now - progressMark >= RESET_PROGRESS_BYTES -> { progressMark = now; stalledAttempts = 0 }
                else -> {
                    stalledAttempts++
                    if (stalledAttempts >= MAX_STALLED_ATTEMPTS) {
                        throw IOException("téléchargement bloqué : $stalledAttempts tentatives sans progrès notable ($now octets)")
                    }
                }
            }

            val delayMs = (RETRY_BASE_DELAY_MS * (stalledAttempts + 1)).coerceAtMost(RETRY_MAX_DELAY_MS)
            Log.d("TWKT", "AppUpdateDownloader: retry #$attempts dans ${delayMs}ms (have=$now, stalled=$stalledAttempts)")
            sleepInterruptible(delayMs, gen)
        }
    }

    private fun sleepInterruptible(ms: Long, gen: Long) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            if (cancelRequested || currentId != gen) return
            Thread.sleep(200)
        }
    }

    /**
     * Une passe de téléchargement. Suit les redirections manuellement
     * (Location header) en ré-appliquant l'en-tête Range à chaque saut.
     *
     * Cause RÉELLE du 404 trouvée le 31/07/2026 (box mediouni) : le premier
     * GET vers github.com/.../releases/download/... échouait déjà en 404,
     * AVANT toute redirection -- pas un problème de suivi de redirection, mais
     * l'absence d'en-tête User-Agent (HttpURLConnection n'en envoie aucun par
     * défaut). fetchRemoteVersion() et curl réussissaient sur la même URL ->
     * GitHub/son CDN filtre les requêtes sans UA reconnaissable sur ce point
     * d'accès. Le suivi manuel des redirections est conservé (plus robuste).
     *
     * @return true si le flux s'est terminé proprement (read == -1) ET que le
     *   fichier atteint la taille totale attendue ; false si le flux s'est
     *   terminé mais qu'il reste des octets à récupérer (l'appelant reboucle).
     * @throws HttpDownloadException code HTTP inattendu (dont 416).
     * @throws IOException coupure réseau / timeout en cours de transfert.
     */
    private fun downloadAttempt(context: Context, url: String, dest: File, gen: Long): Boolean {
        var currentUrl = url
        var redirectCount = 0
        val haveBytes = dest.length()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val validator = prefs.getString(PREF_DOWNLOAD_VALIDATOR, null)

        while (true) {
            val connection = URL(currentUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            // 60s : constaté (31/07/2026, box mediouni) que la connexion de
            // cette box stalle parfois > 20s en cours de transfert (débit
            // faible/instable). Au-delà -> SocketTimeoutException, l'appelant
            // reprend via Range.
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Tawkit-Android")
            connection.setRequestProperty("Accept-Encoding", "identity")
            if (haveBytes > 0L) {
                connection.setRequestProperty("Range", "bytes=$haveBytes-")
                if (!validator.isNullOrBlank()) connection.setRequestProperty("If-Range", validator)
            }

            try {
                connection.connect()
                val code = connection.responseCode
                Log.d("TWKT", "AppUpdateDownloader: GET $currentUrl range=$haveBytes -> $code (${connection.responseMessage})")

                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrBlank()) throw HttpDownloadException(code, "redirect sans Location")
                    if (++redirectCount > 5) throw HttpDownloadException(code, "trop de redirections")
                    currentUrl = location
                    continue
                }
                if (code == 416) throw HttpDownloadException(416, "Range non satisfiable")
                if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                    throw HttpDownloadException(code, "HTTP $code")
                }

                // append = on complète le partiel (206) ; sinon on repart de 0
                // (200 : Range/If-Range ignoré ou ressource modifiée, ou haveBytes == 0).
                val append = (code == HttpURLConnection.HTTP_PARTIAL && haveBytes > 0L)

                if (append) {
                    // Content-Range: bytes 37000000-95999999/96000000
                    val cr = connection.getHeaderField("Content-Range")
                    val total = cr?.substringAfterLast('/', "")?.trim()?.toLongOrNull()
                    currentTotal = when {
                        total != null && total > 0L -> total
                        connection.contentLengthLong > 0L -> haveBytes + connection.contentLengthLong
                        else -> currentTotal
                    }
                } else {
                    currentTotal = connection.contentLengthLong
                    if (haveBytes > 0L) {
                        Log.d("TWKT", "AppUpdateDownloader: 200 (Range/If-Range non honoré ou APK modifié) -> reprise de zéro")
                    }
                    // Nouveau départ : mémoriser le validateur de CETTE version
                    // pour les reprises ultérieures.
                    val newValidator = connection.getHeaderField("ETag")
                        ?: connection.getHeaderField("Last-Modified")
                    prefs.edit().apply {
                        if (!newValidator.isNullOrBlank()) putString(PREF_DOWNLOAD_VALIDATOR, newValidator)
                        else remove(PREF_DOWNLOAD_VALIDATOR)
                    }.apply()
                }

                var written = if (append) haveBytes else 0L
                currentBytes = written

                connection.inputStream.use { input ->
                    FileOutputStream(dest, append).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (cancelRequested) throw IOException("cancelled")
                            if (currentId != gen) throw IOException("superseded")
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read
                            currentBytes = written
                        }
                        output.flush()
                    }
                }

                val total = currentTotal
                return total <= 0L || dest.length() >= total
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Récupère la taille totale du fichier distant sans le télécharger
     * (Range: bytes=0-0 -> Content-Range: bytes 0-0/TOTAL, ou contentLength
     * d'une réponse 200). Sert à trancher un 416 : partiel déjà complet vs
     * partiel périmé. Best-effort : renvoie -1 si indéterminable.
     */
    private fun probeTotal(url: String): Long {
        var currentUrl = url
        try {
            repeat(6) {
                val c = URL(currentUrl).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000
                c.readTimeout = 15_000
                c.instanceFollowRedirects = false
                c.setRequestProperty("User-Agent", "Tawkit-Android")
                c.setRequestProperty("Range", "bytes=0-0")
                try {
                    c.connect()
                    val code = c.responseCode
                    if (code in 300..399) {
                        val loc = c.getHeaderField("Location")
                        if (loc.isNullOrBlank()) return -1L
                        currentUrl = loc
                        return@repeat
                    }
                    val cr = c.getHeaderField("Content-Range")
                    val fromRange = cr?.substringAfterLast('/', "")?.trim()?.toLongOrNull()
                    if (fromRange != null && fromRange > 0L) return fromRange
                    val cl = c.contentLengthLong
                    return if (code == HttpURLConnection.HTTP_OK && cl > 0L) cl else -1L
                } finally {
                    c.disconnect()
                }
            }
            return -1L
        } catch (e: Exception) {
            Log.d("TWKT", "AppUpdateDownloader: probeTotal failed: ${e.message}")
            return -1L
        }
    }

    /** Annule un téléchargement en cours (supprime aussi le fichier partiel). */
    fun cancel(context: Context, id: Long) {
        if (currentId == id) cancelRequested = true
        clearPendingDownloadId(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(PREF_DOWNLOAD_URL)
            .remove(PREF_DOWNLOAD_TOTAL)
            .remove(PREF_DOWNLOAD_VALIDATOR)
            .apply()
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
        // L'installateur systeme va occuper le premier plan potentiellement
        // longtemps (l'utilisateur doit confirmer) -- ne doit pas etre
        // combattu par le watchdog "kiosque" TV (cf. MainActivity.onStop()).
        MainActivity.expectSystemHandoff(graceMs = 5 * 60_000L)
        context.startActivity(installIntent)
    }
}
