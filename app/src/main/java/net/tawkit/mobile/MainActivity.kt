package net.tawkit.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        /** Vrai entre onResume et onPause. Lu par AzanPlaybackService pour ne
         *  pas rejouer l'azan en natif (MediaPlayer) quand l'appli est deja au
         *  premier plan et que le WebView (m2body.js) joue deja son propre
         *  <audio> -> evite le double azan (un son coupe seulement en
         *  retournant le telephone, l'autre seulement en fermant la popup
         *  azan dans l'appli). */
        @Volatile
        var isAppInForeground = false

        /** Extra pose par BootReceiver sur telephone (jamais sur TV) : demande
         *  un lancement silencieux (theme invisible, pas de popups systeme,
         *  auto-finish quelques secondes plus tard) juste pour recalculer les
         *  heures du jour et reprogrammer les alarmes natives. */
        const val EXTRA_SILENT_BOOT = "net.tawkit.mobile.SILENT_BOOT"

        private const val SILENT_BOOT_FINISH_DELAY_MS = 8000L

        /** Filet de securite : si onPageFinished/onReceivedError ne
         *  declenchent jamais (webView bloque), on masque quand meme
         *  l'ecran de chargement plutot que de laisser l'appli figee dessus
         *  indefiniment. */
        private const val SPLASH_FALLBACK_TIMEOUT_MS = 12000L
    }

    private lateinit var webView: WebView
    private lateinit var splashOverlay: View
    private val mainHandler = Handler(Looper.getMainLooper())
    private var splashHidden = false

    /** mosque_id reçu via tap notification avant que la page soit chargée */
    private var pendingConfigSync: String? = null
    /** mosque_id reçu via deep link tawkit://mosque/<id> avant que la page soit chargée */
    private var pendingMosqueDeepLink: String? = null
    private var isPageLoaded = false
    private var automaticUpdateCheckStarted = false

    /** Lancement silencieux post-boot (telephone) : cf. EXTRA_SILENT_BOOT. */
    private var isSilentBoot = false

    /** Horodatage du dernier onPause() ; 0 = jamais mis en pause depuis le
     *  lancement. Sert a ne declencher le resync JS (onResume) que si la
     *  pause a dure assez longtemps pour avoir reellement suspendu les
     *  timers via pauseTimers() -- evite de casser une popup azan/iqama en
     *  cours pour un aller-retour trivial (ex. dialogue systeme). */
    private var pausedAtMs = 0L
    private val RESYNC_THRESHOLD_MS = 5000L

    // Request notification permission (Android 13+)
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("TWKT", "Notification permission granted: $granted")
    }

    // Picker SAF pour importer des recitateurs Coran depuis une cle USB / stockage externe
    private val importTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            Log.d("TWKT", "Reciter import: picker cancelled")
            return@registerForActivityResult
        }
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.e("TWKT", "takePersistableUriPermission failed: ${e.message}")
        }
        CoroutineScope(Dispatchers.IO).launch {
            ReciterManager.importFromTree(this@MainActivity, uri)
        }
    }

    // Sélecteur de fichier standard WebView (<input type="file">, cf. custom.js
    // sélecteur photo mosquée) : sans onShowFileChooser, ces clics ne font
    // RIEN silencieusement — c'est pour ça que l'export/import de config
    // basculait sur une modale "coller le JSON" côté Android au lieu du
    // vrai sélecteur (cf. _installConfigBackup, custom.js). Résolu ici une
    // fois pour toutes : ouvre le sélecteur système (galerie/fichiers +
    // appareil photo si <input capture> et matériel disponible).
    private var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var cameraCaptureUri: Uri? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFileChooserCallback
        pendingFileChooserCallback = null
        if (callback == null) return@registerForActivityResult

        val results: Array<Uri>? = when {
            result.resultCode != RESULT_OK -> null
            // Photo prise via l'appareil photo : l'intent de résultat ne contient
            // pas l'URI (elle a été fixée à l'avance via EXTRA_OUTPUT), et la
            // galerie n'a rien retourné non plus -> c'est la capture caméra.
            result.data?.dataString == null && cameraCaptureUri != null -> arrayOf(cameraCaptureUri!!)
            else -> WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        }
        cameraCaptureUri = null
        callback.onReceiveValue(results)
    }

    /** URI content:// (FileProvider, même autorité que les mises à jour APK)
     *  pour le fichier où l'appareil photo système va écrire la capture. */
    private fun createCameraCaptureUri(): Uri? {
        return try {
            val dir = File(cacheDir, "camera_captures").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) {
            Log.e("TWKT", "createCameraCaptureUri failed: ${e.message}")
            null
        }
    }

    // Géolocalisation : callback du WebView en attente pendant la demande de
    // permission OS (bouton "ma position" / carte Google Maps, cf. custom.js)
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("TWKT", "Location permission granted: $granted")
        val origin = pendingGeoOrigin
        val callback = pendingGeoCallback
        pendingGeoOrigin = null
        pendingGeoCallback = null
        callback?.invoke(origin ?: "", granted, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        isSilentBoot = intent?.getBooleanExtra(EXTRA_SILENT_BOOT, false) ?: false
        // Doit etre appele AVANT super.onCreate()/setContentView() pour que la
        // fenetre se cree directement invisible (aucun flash visuel possible).
        if (isSilentBoot) setTheme(R.style.Theme_TawkitMobile_Invisible)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splashOverlay = findViewById(R.id.splashOverlay)

        if (isSilentBoot) {
            // Fenetre deja invisible (theme) : l'ecran de chargement natif
            // n'a aucune utilite et ne doit surtout pas etre revele plus
            // tard (il n'y a pas de onPageFinished visuel a attendre ici).
            splashOverlay.visibility = View.GONE
        } else {
            mainHandler.postDelayed({ hideSplash() }, SPLASH_FALLBACK_TIMEOUT_MS)
        }

        // Ces trois appels affichent des popups systeme (permissions, OneSignal) :
        // sauter en lancement silencieux, sinon on recree exactement le probleme
        // qu'EXTRA_SILENT_BOOT est censee eviter.
        if (!isSilentBoot) {
            initOneSignal()
            requestNotificationPermission()
            requestIgnoreBatteryOptimizations()
        }
        setupWebView()
        handleMosqueDeepLinkIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Sécurité boîtier TV : sur certains boîtiers, un simple clic droit
                    // de souris (mappé sur "retour") suffit à déclencher cet appel, souvent
                    // par inadvertance. Puisqu'aucune modale JS n'est plus ouverte à ce
                    // stade (webView.canGoBack() == false, cf. _installBackManager dans
                    // custom.js qui gère déjà la fermeture des modales), une confirmation
                    // explicite évite de fermer l'app par erreur.
                    // Neutre "Paramètres Android" : quand Tawkit est le launcher (boîtier
                    // TV configuré en app par défaut de l'écran d'accueil), quitter relance
                    // simplement l'app (comportement normal d'un launcher, laissé tel quel)
                    // et il n'existe alors plus aucun autre moyen d'atteindre les réglages
                    // système — ce bouton reste la seule porte de sortie vers Android.
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Quitter l'application ?")
                        .setMessage("Voulez-vous vraiment fermer Tawkit ?")
                        .setNegativeButton("Annuler", null)
                        .setNeutralButton("Paramètres Android") { _, _ ->
                            try {
                                startActivity(
                                    Intent(Settings.ACTION_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) {
                                Log.e("TWKT", "Unable to open Android settings", e)
                            }
                        }
                        .setPositiveButton("Quitter") { _, _ ->
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                        .show()
                }
            }
        })

        // Load app from assets (offline-first)
        webView.loadUrl("file:///android_asset/index.html")

        if (isSilentBoot) {
            // Repli temporel plutot qu'un callback JS->natif de "fin de
            // reprogrammation" : meme principe deja utilise par
            // _installNativeAzanAlarms (custom.js), qui laisse 4.5s a
            // calculateAndDisplayTimesFunction avant d'envoyer les alarmes au
            // pont natif. 8s laisse une marge confortable, puis on ferme sans
            // laisser de trace dans les apps recentes.
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("TWKT", "Silent boot: closing after reschedule window")
                finishAndRemoveTask()
            }, SILENT_BOOT_FINISH_DELAY_MS)
        }
    }

    private fun initOneSignal() {
        OneSignal.Debug.logLevel = LogLevel.WARN
        OneSignal.initWithContext(this, "a7656f67-9573-4593-97a8-871ac6550731")
        // Demande la permission push de façon non-bloquante
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }
        // Tap notification → sync config dans le WebView
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data     = event.notification.additionalData
                val type     = data?.optString("type", "")      ?: ""
                val mosqueId = data?.optString("mosque_id", "") ?: ""
                if (type == "config_update" && mosqueId.isNotEmpty()) {
                    Log.d("TWKT", "Notification tapped: config_update for $mosqueId")
                    runOnUiThread { dispatchConfigSync(mosqueId) }
                }
            }
        })
        Log.d("TWKT", "OneSignal initialized")
    }

    /** Dispatch l'event ucConfigSync vers le WebView.
     *  Si la page n'est pas encore chargée, stocke dans pendingConfigSync. */
    private fun dispatchConfigSync(mosqueId: String) {
        if (!isPageLoaded) {
            pendingConfigSync = mosqueId
            Log.d("TWKT", "Page not loaded yet, saving mosqueId: $mosqueId")
            return
        }
        val safe = mosqueId.replace("'", "\\'")
        webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('ucConfigSync',{detail:{mosque_id:'$safe'}}));",
            null
        )
        Log.d("TWKT", "Dispatched ucConfigSync for $mosqueId")
    }

    /**
     * Deep link "Ouvrir cette mosquée dans l'application" (bloc Partager de
     * la fiche mosquée, custom.js _shareMosqueInfo) : tawkit://mosque/<id>,
     * capté via l'intent-filter VIEW/BROWSABLE (AndroidManifest.xml). Extrait
     * l'id puis dispatch un event dédié 'ucMosqueDeepLink' — DIFFÉRENT de
     * 'ucConfigSync' (qui ne fait qu'une synchronisation légère de la table
     * "mosques") : ici on veut un import COMPLET de la config (comme une
     * sélection dans le sélecteur de mosquée), géré côté JS par
     * window._ucHandleMosqueDeepLink (_installConfigBackup, custom.js).
     */
    private fun handleMosqueDeepLinkIntent(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == "tawkit" && uri.host == "mosque") {
            val mosqueId = uri.lastPathSegment
            if (!mosqueId.isNullOrBlank()) {
                Log.d("TWKT", "Mosque deep link received: $mosqueId")
                dispatchMosqueDeepLink(mosqueId)
            }
        }
    }

    /** Si la page n'est pas encore chargée, stocke dans pendingMosqueDeepLink. */
    private fun dispatchMosqueDeepLink(mosqueId: String) {
        if (!isPageLoaded) {
            pendingMosqueDeepLink = mosqueId
            Log.d("TWKT", "Page not loaded yet, saving mosque deep link: $mosqueId")
            return
        }
        val safe = mosqueId.replace("'", "\\'")
        webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('ucMosqueDeepLink',{detail:{mosque_id:'$safe'}}));",
            null
        )
        Log.d("TWKT", "Dispatched ucMosqueDeepLink for $mosqueId")
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    private fun setupWebView() {
        // Active chrome://inspect (Chrome DevTools distant) sur ce WebView --
        // permet de brancher un vrai eval JS/console/DOM inspector via un
        // téléphone connecté en USB (adb), sans quoi le diagnostic de bugs
        // custom.js en conditions réelles se limite aux traces _L()/logcat
        // (cf. rapports debug Supabase). App 100% offline, contenu local de
        // confiance uniquement : aucun risque à l'activer inconditionnellement
        // (l'accès requiert de toute façon une connexion adb autorisée sur CE
        // téléphone, pas un vecteur d'attaque distant).
        WebView.setWebContentsDebuggingEnabled(true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true           // localStorage (prayer settings)
            mediaPlaybackRequiresUserGesture = false  // auto-play azan audio
            // LOAD_DEFAULT laissait WebView mettre en cache les ressources
            // file:///android_asset/ (custom.js, etc.) dans le stockage privé
            // de l'app, lequel SURVIT à une mise à jour APK (install par-dessus
            // l'existant, sans désinstallation) : après une mise à jour, le
            // WebView pouvait continuer à servir une version en cache de
            // custom.js plus ancienne que celle réellement présente dans le
            // nouvel APK (constaté : ReferenceError sur une fonction ajoutée
            // depuis, alors que le fichier sur disque était pourtant à jour).
            // Ces assets sont 100% locaux (aucun gain réseau à mettre en
            // cache) -> LOAD_NO_CACHE force systématiquement une lecture
            // fraîche, garantissant qu'une mise à jour de l'appli est toujours
            // reflétée immédiatement, sans qu'une désinstallation complète soit
            // nécessaire.
            cacheMode = WebSettings.LOAD_NO_CACHE
            // Par défaut, le WebView applique le réglage système "Taille de
            // police" (accessibilité Android, Configuration.fontScale) sur le
            // texte de la page, en plus de notre propre CSS. Cette appli est
            // un affichage à mise en page fixe (proportions vw/vh calculées
            // pour un rendu type kiosk — cf. _fixAyaFontSizeWideScreen dans
            // custom.js) : le multiplicateur système vient s'ajouter à nos
            // calculs et fait déborder/chevaucher certains éléments quand
            // l'utilisateur agrandit la police du téléphone. On fige le rendu
            // du texte à 100%, indépendamment du réglage système — la taille
            // de police reste entièrement pilotée par notre CSS/JS.
            textZoom = 100
            // Zoom WebView natif désactivé : le lecteur Coranique gère le
            // pincement lui-même via JS (touchmove + _qrApplyFontSize).
            // Avec setSupportZoom(true), le WebView intercepte le pincement
            // en natif avant que JS ne reçoive les événements, ce qui
            // écrase notre zoom JS et revient à la taille initiale.
            setSupportZoom(false)
            builtInZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // Page chargée depuis file:///android_asset/index.html. Sans ces
            // réglages, Chromium traite chaque chemin file:// comme une origine
            // distincte et bloque le chargement dans <audio> des récitateurs
            // "device" (USB/téléchargés) situés sous file:///storage/... -> erreur
            // SRC_NOT_SUPPORTED / "[RES] AUDIO load failed" même quand le fichier
            // est valide. App 100% offline, contenu local de confiance uniquement :
            // aucun risque à élargir l'accès file://.
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            // Carte Google Maps embarquée (bouton "ma position") :
            // navigator.geolocation dans le WebView nécessite ce flag en plus
            // de la permission OS gérée par onGeolocationPermissionsShowPrompt.
            setGeolocationEnabled(true)
        }

        // JS Bridge: page calls window.AndroidMobile.scheduleNotification(...)
        webView.addJavascriptInterface(
            MobileJsBridge(
                this,
                onRequestImport = { importTreeLauncher.launch(null) },
                onSetKeepScreenOn = { enabled -> setKeepScreenOn(enabled) },
                onCheckForUpdate = {
                    runOnUiThread { AppUpdateChecker.check(this, manual = true) }
                },
                onOpenTvUtilityMenu = {
                    runOnUiThread { showTvUtilityMenu() }
                }
            ),
            "AndroidMobile"
        )

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.d("TWKT", "[${msg.messageLevel()}] ${msg.message()}")
                return true
            }
            // <input type="file"> (galerie/fichiers + appareil photo si dispo) —
            // cf. pendingFileChooserCallback ci-dessus pour le contexte.
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                pendingFileChooserCallback?.onReceiveValue(null)
                pendingFileChooserCallback = filePathCallback

                val intents = mutableListOf<Intent>()
                // Appareil photo, uniquement si le matériel existe (absent sur la
                // plupart des box TV — l'intent est alors simplement omis, pas
                // d'entrée "Appareil photo" dans le sélecteur système).
                if (packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)) {
                    val captureUri = createCameraCaptureUri()
                    if (captureUri != null) {
                        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        }
                        if (captureIntent.resolveActivity(packageManager) != null) {
                            cameraCaptureUri = captureUri
                            intents.add(captureIntent)
                        }
                    }
                }

                val chooserTarget = fileChooserParams.createIntent()
                val chooser = Intent.createChooser(chooserTarget, "اختر صورة | Choisir une image")
                if (intents.isNotEmpty()) {
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
                }

                return try {
                    fileChooserLauncher.launch(chooser)
                    true
                } catch (e: Exception) {
                    Log.e("TWKT", "onShowFileChooser launch failed: ${e.message}")
                    pendingFileChooserCallback = null
                    false
                }
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
            // Bouton "ma position" (custom.js) -> navigator.geolocation.
            // Vérifie la permission OS ACCESS_FINE_LOCATION ; la demande via
            // le dialogue système si pas déjà accordée. retain=false : on ne
            // mémorise pas la décision côté WebView, pour toujours refléter
            // l'état réel de la permission système au prochain appel (utile
            // si l'utilisateur l'active plus tard depuis les réglages Android).
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            // Sans cet override, le WebView tente de NAVIGUER lui-meme vers les
            // schemas non http(s) (tel:, mailto:, sms:, geo:...) utilises par
            // la fiche mosquee (appel/email, cf. custom.js _wireMosqueProfileBlock)
            // et affiche une page d'erreur au lieu d'ouvrir l'appli native
            // correspondante (composeur, mail...). On laisse passer uniquement
            // les schemas de la page elle-meme (file:// pour les assets locaux,
            // http/https pour d'eventuelles ressources distantes) ; tout le
            // reste est delegue a une Intent ACTION_VIEW standard.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                when (uri.scheme) {
                    "http", "https", "file", "about", "data", "blob" -> return false
                }
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    true
                } catch (e: Exception) {
                    Log.e("TWKT", "shouldOverrideUrlLoading: no handler for $uri (${e.message})")
                    true
                }
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                Log.e("TWKT", "Error: ${err.description} for ${req.url}")
                if (req.isForMainFrame) hideSplash()
            }
            // Cold-start via tap notification : dispatch ucConfigSync après chargement complet
            override fun onPageFinished(view: WebView, url: String) {
                isPageLoaded = true
                hideSplash()
                val mid = pendingConfigSync
                if (mid != null) {
                    pendingConfigSync = null
                    view.postDelayed({ dispatchConfigSync(mid) }, 1200)
                }
                val deepLinkMid = pendingMosqueDeepLink
                if (deepLinkMid != null) {
                    pendingMosqueDeepLink = null
                    view.postDelayed({ dispatchMosqueDeepLink(deepLinkMid) }, 1200)
                }
                if (!automaticUpdateCheckStarted) {
                    automaticUpdateCheckStarted = true
                    view.postDelayed(
                        { AppUpdateChecker.maybeAutoCheck(this@MainActivity) },
                        1500
                    )
                }
                maybeShowAutoStartSetupPrompt(view)
                maybeShowTvHomeLauncherPrompt(view)
            }
        }
    }

    /**
     * Masque l'ecran de chargement natif (logo + basmala + spinner) affiche
     * par-dessus le WebView pendant l'analyse des scripts et le premier
     * rendu. Idempotent (isPageLoaded/onReceivedError/le filet de securite
     * peuvent chacun l'appeler) : ne joue le fondu qu'une seule fois.
     */
    private fun hideSplash() {
        if (splashHidden) return
        splashHidden = true
        mainHandler.removeCallbacksAndMessages(null)
        splashOverlay.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction { splashOverlay.visibility = View.GONE }
            .start()
    }

    /**
     * Demande une seule fois, au premier lancement reel (jamais en lancement
     * silencieux, jamais sur TV — cf. AutoStartPrefs), si l'utilisateur veut
     * que l'appli redemarre automatiquement (silencieusement) apres un
     * redemarrage du telephone. Le reglage reste modifiable ensuite depuis
     * l'onglet الإعدادات (cf. custom.js / MobileJsBridge.setAutoStartEnabled).
     */
    private fun maybeShowAutoStartSetupPrompt(view: WebView) {
        if (isSilentBoot) return
        if (AutoStartPrefs.hasAskedSetup(this)) return
        if (DeviceType.isAndroidTv(this)) return
        view.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.autostart_prompt_title))
                .setMessage(getString(R.string.autostart_prompt_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.autostart_prompt_yes)) { _, _ ->
                    AutoStartPrefs.setEnabled(this, true)
                    AutoStartPrefs.markSetupAsked(this)
                }
                .setNegativeButton(getString(R.string.autostart_prompt_no)) { _, _ ->
                    AutoStartPrefs.setEnabled(this, false)
                    AutoStartPrefs.markSetupAsked(this)
                }
                .show()
        }, 1200)
    }

    /**
     * Demande une seule fois, au premier lancement reel sur boitier Android
     * TV, si l'utilisateur veut definir Tawkit comme ecran d'accueil
     * (HOME). C'est le moyen le plus fiable de garantir un demarrage
     * automatique : le systeme lance TOUJOURS l'appli d'accueil par defaut
     * au boot, contrairement au BootReceiver (BOOT_COMPLETED) qui peut etre
     * retarde/bloque par certains firmwares generiques (constate sur
     * X88 Pro 20). Les deux mecanismes restent actifs en parallele.
     */
    private fun maybeShowTvHomeLauncherPrompt(view: WebView) {
        if (isSilentBoot) return
        if (!DeviceType.isAndroidTv(this)) return
        if (TvHomeLauncherPrefs.hasAskedSetup(this)) return
        view.postDelayed({ showTvHomeLauncherDialog() }, 1800)
    }

    /**
     * Affiche le dialogue de choix "ecran d'accueil". Appelable une seule
     * fois automatiquement (premier lancement, cf.
     * maybeShowTvHomeLauncherPrompt) ou a volonte ensuite via
     * MobileJsBridge.reopenTvHomeLauncherPrompt() — il n'existe pas
     * d'ecran de reglages accessible en mode horizontal (TV) dans le
     * WebView pour reproposer ce choix autrement.
     */
    fun showTvHomeLauncherDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.tv_home_launcher_prompt_title))
            .setMessage(getString(R.string.tv_home_launcher_prompt_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.tv_home_launcher_prompt_yes)) { _, _ ->
                TvHomeLauncherPrefs.setEnabled(this, true)
                TvHomeLauncherPrefs.markSetupAsked(this)
                TvHomeLauncherHelper.setAliasEnabled(this, true)
                TvHomeLauncherHelper.openHomeAppPicker(this)
            }
            .setNegativeButton(getString(R.string.tv_home_launcher_prompt_no)) { _, _ ->
                TvHomeLauncherPrefs.setEnabled(this, false)
                TvHomeLauncherPrefs.markSetupAsked(this)
                TvHomeLauncherHelper.setAliasEnabled(this, false)
            }
            .show()
    }

    /**
     * Menu "outils" boitier TV, ouvert par un appui long sur le logo (cf.
     * MobileJsBridge.openTvUtilityMenu / custom.js). Une fois Tawkit defini
     * comme ecran d'accueil, le bouton Accueil de la telecommande ne mene
     * plus qu'a Tawkit lui-meme : ce menu est le seul moyen restant
     * d'atteindre les reglages Android (Wi-Fi, etc.) sans desactiver
     * l'autostart. android.intent.category.HOME n'est jamais retire ici —
     * l'utilisateur revient toujours a Tawkit au prochain appui Accueil.
     */
    fun showTvUtilityMenu() {
        if (isFinishing || isDestroyed) return
        val items = arrayOf(
            getString(R.string.tv_menu_open_android_settings),
            getString(R.string.tv_menu_change_home_launcher)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.tv_menu_title))
            .setItems(items) { _, which ->
                when (which) {
                    0 -> try {
                        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Log.e("TWKT", "ACTION_SETTINGS unavailable: ${e.message}")
                    }
                    1 -> showTvHomeLauncherDialog()
                }
            }
            .show()
    }

    /**
     * Empeche/autorise l'ecran de s'eteindre ou de passer en veille pendant
     * que l'appli est au premier plan. Pilote depuis custom.js pendant un
     * telechargement de recitateur (cf. MobileJsBridge.setKeepScreenOn) :
     * l'extinction d'ecran coupe le worker WorkManager en cours. Les methodes
     * @JavascriptInterface peuvent etre appelees depuis un thread JS qui n'est
     * pas le thread UI -> runOnUiThread est obligatoire ici.
     */
    private fun setKeepScreenOn(enabled: Boolean) {
        runOnUiThread {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Demande l'exemption d'optimisation de batterie (Doze / App Standby).
     * Sans ça, Android peut limiter le reveil de l'appli en arriere-plan et
     * gener les alarmes/notifications sur un usage continu (ecran mural
     * mosquee). Si deja accordee, isIgnoringBatteryOptimizations renvoie
     * true et on ne fait rien -> pas de popup repetee a chaque lancement
     * une fois que l'utilisateur a accepte.
     */
    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("TWKT", "requestIgnoreBatteryOptimizations failed: ${e.message}")
        }
    }

    /* Removed deprecated onBackPressed */

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        // Reprend l'exécution JS du WebView (cf. onPause ci-dessous) : sans ça,
        // au retour au premier plan, la page resterait figée sur son dernier
        // état d'avant la mise en veille.
        webView.onResume()
        webView.resumeTimers()
        NativeEventLog.log(this, "AZAN", "APP_RESUME")
        maybeTriggerJsResync()
        maybeReassertTvHomeLauncher()
    }

    /**
     * Déclenche le resync JS (_ucResyncPrayerSequence dans custom.js) depuis
     * le natif plutôt que de compter sur `document.visibilitychange` côté
     * WebView, qui ne se synchronise pas de façon fiable avec le cycle de vie
     * de l'Activity (onPause/onResume) — c'est pourtant onPause/onResume qui
     * suspend/reprend réellement les timers JS via pauseTimers()/
     * resumeTimers() ci-dessus. Sans ce déclenchement natif, l'app pouvait
     * rester figée sur l'état d'avant la mise en arrière-plan après retour.
     *
     * Seuil dupliqué de HIDDEN_THRESHOLD_MS (custom.js, _installResyncOnResume)
     * — garder les deux en phase si l'un des deux change.
     */
    private fun maybeTriggerJsResync() {
        val pausedAt = pausedAtMs
        pausedAtMs = 0L
        if (pausedAt == 0L) return // jamais mis en pause depuis le lancement
        val hiddenMs = System.currentTimeMillis() - pausedAt
        if (hiddenMs <= RESYNC_THRESHOLD_MS) return
        webView.evaluateJavascript(
            "if (window._ucResyncPrayerSequence) window._ucResyncPrayerSequence('native_resume');",
            null
        )
    }

    /**
     * Si l'utilisateur avait choisi Tawkit comme ecran d'accueil
     * (TvHomeLauncherPrefs) mais que ce choix a ete perdu (retour au
     * launcher d'origine, reglages "Applications par defaut" modifies,
     * etc.), rouvre automatiquement le selecteur systeme des que Tawkit
     * revient au premier plan — l'utilisateur n'a plus qu'a re-taper
     * "Tawkit" une fois, sans avoir besoin de se souvenir du geste d'appui
     * long sur le logo. Ne s'applique jamais si l'utilisateur n'a pas
     * explicitement active ce reglage, ni sur telephone, ni en boot
     * silencieux.
     */
    private fun maybeReassertTvHomeLauncher() {
        if (isSilentBoot) return
        if (!DeviceType.isAndroidTv(this)) return
        if (!TvHomeLauncherPrefs.isEnabled(this)) return
        if (TvHomeLauncherHelper.isCurrentlyDefaultHome(this)) return
        Log.d("TWKT", "TV home launcher preference lost — reopening picker")
        TvHomeLauncherHelper.setAliasEnabled(this, true)
        TvHomeLauncherHelper.openHomeAppPicker(this)
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
        // Suspend reellement l'execution JS du WebView (setInterval de
        // m2body.js inclus) quand l'appli passe en arriere-plan (ecran
        // verrouille/eteint pendant que l'appli reste ouverte, changement
        // d'appli, etc.). SANS CA, le WebView continuait a tourner et a
        // jouer son propre <audio> de l'azan meme ecran eteint, EN PLUS de
        // AzanPlaybackService (declenche par l'alarme AlarmManager native,
        // qui verifie isAppInForeground == false et joue donc lui aussi) ->
        // double azan reel (les deux lectures simultanees), le garde-fou de
        // AzanPlaybackService reposant sur l'hypothese - fausse jusqu'ici -
        // que le WebView backgrounded ne joue plus rien.
        webView.onPause()
        webView.pauseTimers()
        pausedAtMs = System.currentTimeMillis()
        NativeEventLog.log(this, "AZAN", "APP_PAUSE")
    }

    // Called when notification is tapped and app is already open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val prayer = intent.getStringExtra("prayer")
        if (prayer != null) {
            webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('notificationTap', {detail: {prayer: '$prayer'}}))",
                null
            )
        }
        // Fallback : si OneSignal passe mosque_id via Intent extras
        val mosqueId = intent.getStringExtra("mosque_id")
        if (mosqueId != null) {
            dispatchConfigSync(mosqueId)
        }
        handleMosqueDeepLinkIntent(intent)
    }
}
