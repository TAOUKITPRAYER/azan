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
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
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
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.lang.ref.WeakReference

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

        /** Filet de securite ultime : si MobileJsBridge.notifyAppFullyReady()
         *  ne se declenche jamais (JS bloque/crash), on masque quand meme
         *  l'ecran de chargement plutot que de laisser l'appli figee dessus
         *  indefiniment. Valeur mesuree en conditions reelles (logcat,
         *  20/08/2026, boitier Mediouni) : le chargement complet reel
         *  (evenement 'load' de la fenetre, qui attend entre autres la
         *  cinquantaine de probes d'images de themes absentes et une
         *  tentative de connexion au serveur audio local de la box) prend
         *  ~18-20s sur ce boitier -- l'ancienne valeur de 12s (calibree a
         *  l'epoque pour onPageFinished, un signal beaucoup plus precoce)
         *  faisait donc declencher ce filet de secours SYSTEMATIQUEMENT avant
         *  le vrai signal JS, masquant totalement le nouveau mecanisme.
         *  Remontee a 30s : large marge au-dessus du cas reel observe... mais
         *  ce cas reel avait ete mesure via un simple kill -9 + respawn du
         *  process (reste du systeme deja demarre et inactif). Sur un VRAI
         *  redemarrage a froid du boitier (mise sous tension), constate le
         *  20/08/2026 via reboot complet + logcat : tout le systeme Android
         *  demarre en meme temps (autres apps/services, I/O disque en
         *  contention), et le chargement reel de la page a pris ~35s au lieu
         *  de ~18-20s -- ce filet de 30s se declenchait donc a nouveau AVANT
         *  le vrai signal JS (hideSplash() a 22:57:03.846, alors que
         *  _LOAD_COMPLETED_ n'est arrive qu'a 22:57:07.963, soit 4s trop
         *  tard), reproduisant exactement le bug que ce filet est cense
         *  eviter. Remontee a 60s pour couvrir ce cas de demarrage a froid
         *  reel avec marge, tout en restant un delai fini en cas de blocage
         *  genuine. */
        private const val SPLASH_FALLBACK_TIMEOUT_MS = 60000L

        /** Reference faible vers l'instance vivante (posee dans onCreate) --
         *  permet a un BroadcastReceiver independant (TimeChangeReceiver) de
         *  declencher un appel JS sans dependre du cycle de vie de l'Activity.
         *  WeakReference : pas besoin de la nettoyer explicitement dans
         *  onDestroy, elle ne peut pas retenir l'Activity en memoire. */
        @Volatile
        private var instanceRef: WeakReference<MainActivity>? = null

        /**
         * Reprogramme les alarmes azan natives cote JS (_ucRescheduleNativeAzanAlarms,
         * cf. custom.js _installNativeAzanAlarms -> _sendToNative) -- appelee par
         * TimeChangeReceiver quand ACTION_TIME_CHANGED est recu (typiquement la
         * synchronisation NTP automatique juste apres l'obtention d'une connexion
         * internet, notamment au demarrage a froid apres coupure electrique).
         *
         * Necessaire car schedulePrayerNotifications() capture scheduledAtMillis
         * au moment de la programmation en se basant sur l'horloge systeme ALORS
         * courante -- si cette programmation a eu lieu juste apres le boot, AVANT
         * que NTP corrige une horloge fausse (boitiers sans RTC a batterie
         * fiable), l'alarme suivante est armee avec un scheduledAtMillis perime.
         * Quand NTP corrige ensuite l'horloge, PrayerAlarmReceiver.
         * STALE_ALARM_THRESHOLD_MS (garde-fou anti-rafale ajoute le 18/08/2026
         * pour un incident different) compare l'heure reelle a ce
         * scheduledAtMillis perime et ignore silencieusement l'azan -- constate
         * en pratique : azan de la toute premiere priere suivant un redemarrage
         * apres coupure electrique jamais joue, alors que les prieres suivantes
         * (reprogrammees le lendemain avec une horloge deja correcte) fonctionnent
         * normalement. Sans effet si aucune instance vivante ou page pas encore
         * chargee (fallback : la reprogrammation quotidienne UC_EVT.AZAN_TIME
         * reste le seul filet, comme avant ce correctif).
         */
        fun rescheduleNativeAzanAlarmsIfReady() {
            val activity = instanceRef?.get() ?: return
            if (!activity.isPageLoaded) return
            activity.webView.evaluateJavascript(
                "if (window._ucRescheduleNativeAzanAlarms) window._ucRescheduleNativeAzanAlarms();",
                null
            )
        }
    }

    private lateinit var webView: WebView
    private lateinit var splashOverlay: View
    private lateinit var splashProgress: CircularProgressView
    private lateinit var splashProgressText: TextView
    private val mainHandler = Handler(Looper.getMainLooper())
    private var splashHidden = false

    /** mosque_id reçu via tap notification avant que la page soit chargée */
    private var pendingConfigSync: String? = null
    /** mosque_id reçu via deep link tawkit://mosque/<id> avant que la page soit chargée */
    private var pendingMosqueDeepLink: String? = null
    private var isPageLoaded = false
    private var automaticUpdateCheckStarted = false

    /** Boucle de vérification quotidienne (heure choisie par l'admin) de mise
     *  à jour silencieuse, cf. setAutoDailyUpdateEnabled -- remplace le
     *  sondage 60s (désactivé, cf. onPageFinished) comme mécanisme de
     *  production. Nul si désactivée. */
    private var dailySilentUpdateJob: Job? = null

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

    // Picker SAF pour choisir un fichier audio azan personnalise (onglet
    // "تعديل الأذان", blocs "أذان الفجر" / "أذان (باقي الصلوات)", cf.
    // custom.js _acPickCustomFile / MobileJsBridge.pickCustomAzanFile).
    // groupKey ("fajr"/"general") fixe juste avant .launch(), lu au retour.
    private var pendingCustomAzanGroup: String? = null

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val groupKey = pendingCustomAzanGroup
        pendingCustomAzanGroup = null
        if (uri == null || groupKey == null) {
            Log.d("TWKT", "Custom azan file picker: cancelled")
            return@registerForActivityResult
        }
        CoroutineScope(Dispatchers.IO).launch {
            AzanCatalogManager.importCustomFile(this@MainActivity, groupKey, uri)
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
        instanceRef = WeakReference(this)
        isSilentBoot = intent?.getBooleanExtra(EXTRA_SILENT_BOOT, false) ?: false
        // Doit etre appele AVANT super.onCreate()/setContentView() pour que la
        // fenetre se cree directement invisible (aucun flash visuel possible).
        if (isSilentBoot) setTheme(R.style.Theme_TawkitMobile_Invisible)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splashOverlay = findViewById(R.id.splashOverlay)
        splashProgress = findViewById(R.id.splashProgress)
        splashProgressText = findViewById(R.id.splashProgressText)

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
                // Vérification DIRECTE de l'état réel des modales côté JS, avant de
                // se fier à webView.canGoBack() -- retour utilisateur (22/08/2026,
                // téléphone en mode vertical, menu principal ouvert) : la boîte
                // "Quitter l'application ?" s'affichait quand même. Le mécanisme JS
                // existant (history.pushState()/popstate, cf. _installBackManager
                // dans custom.js) est censé faire en sorte que webView.canGoBack()
                // devienne true dans ce cas -- déjà en place pour une demande
                // similaire précédente, mais constaté insuffisant/désynchronisé sur
                // cet appareil. Ce garde-fou natif ne dépend d'aucun état
                // d'historique WebView : il appelle DIRECTEMENT
                // window._ucNativeBackClose() (custom.js), qui ferme la modale de
                // premier plan côté DOM si besoin, AVANT tout recours à
                // canGoBack()/la boîte de dialogue -- fonctionne donc même si le
                // mécanisme JS pushState n'a pas armé l'historique pour une raison
                // quelconque.
                // GÉNÉRALISÉ (23/08/2026, demande explicite : ne plus traiter une
                // modale à la fois) : _ucNativeBackClose() couvre TOUTES les
                // modales de _installBackManager (menu principal, sections de
                // réglages, lecteur Coran, catalogue azan, Qibla, carte, QR code,
                // infos mosquée...) -- ce code natif n'a plus besoin de connaître
                // la liste, ni d'être modifié pour une future modale : l'ajouter au
                // seul endroit qui les connaît déjà (_ucCloseTopmostBackTarget,
                // custom.js) suffit.
                webView.evaluateJavascript(
                    "(function(){try{" +
                        "if(typeof window._ucNativeBackClose==='function'&&window._ucNativeBackClose()){" +
                        "return 'closed';" +
                        "}return 'nothing_open';" +
                    "}catch(e){return 'nothing_open';}})()"
                ) { result ->
                    if (result == "\"closed\"") {
                        return@evaluateJavascript
                    }
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
        // Reception SILENCIEUSE (sans banniere systeme), pour l'administration
        // a distance (cf. rapid-service mode remote_config_update, custom.js
        // _installRemoteMosqueAdmin) : contrairement au tap ci-dessus, qui
        // necessite un humain physiquement devant l'ecran -- inenvisageable
        // pour une box murale sans surveillance -- ce listener s'execute des
        // que la notification est recue tant que le PROCESSUS appli est vivant
        // (premier plan ou arriere-plan recent), ce qui couvre le cas reel de
        // la box (toujours relancee au premier plan, cf. BootReceiver/
        // TvHomeLauncherPrefs). data.silent=true DISTINGUE ce push du push
        // VISIBLE "Mise a jour des horaires" (meme type "config_update",
        // declenche par le trigger SQL on_mosque_update a chaque ecriture
        // mosques -- y compris celles de l'administration a distance elle-
        // meme) : seul silent=true supprime la banniere, l'autre continue de
        // s'afficher normalement (comportement existant, inchange). Si le
        // processus est completement tue (rare pour une box), le polling
        // cote box (filet de securite, cf. custom.js) rattrape sous 15-20s
        // au prochain lancement.
        OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
            override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                val data     = event.notification.additionalData
                val type     = data?.optString("type", "")      ?: ""
                val mosqueId = data?.optString("mosque_id", "") ?: ""
                val silent   = data?.optBoolean("silent", false) ?: false
                if (type == "config_update" && mosqueId.isNotEmpty() && silent) {
                    event.preventDefault()
                    Log.d("TWKT", "Silent config_update received for $mosqueId")
                    runOnUiThread { dispatchConfigSync(mosqueId) }
                } else if (type == "remote_action" && silent) {
                    event.preventDefault()
                    val action = data?.optString("action", "") ?: ""
                    val target = data?.optString("target", "") ?: ""
                    Log.d("TWKT", "Silent remote_action received: $action/$target")
                    runOnUiThread { dispatchRemoteAction(action, target) }
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

    /** Dispatch l'event ucRemoteAction vers le WebView (onglet Actions,
     *  _installRemoteMosqueAdmin -> custom.js listener 'ucRemoteAction').
     *  Contrairement à dispatchConfigSync, PAS de file d'attente si la page
     *  n'est pas encore chargée : une commande ponctuelle ("bascule la
     *  lecture maintenant") rejouée après un délai arbitraire au chargement
     *  suivant n'a plus le même sens que l'intention originale de l'admin --
     *  on l'ignore simplement plutôt que de la reporter. */
    private fun dispatchRemoteAction(action: String, target: String) {
        if (!isPageLoaded || action.isEmpty()) {
            Log.d("TWKT", "Remote action dropped (page not loaded or empty action)")
            return
        }
        val safeAction = action.replace("'", "\\'")
        val safeTarget = target.replace("'", "\\'")
        webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('ucRemoteAction',{detail:{action:'$safeAction',target:'$safeTarget'}}));",
            null
        )
        Log.d("TWKT", "Dispatched ucRemoteAction: $action/$target")
    }

    /** Dispatch le résultat de la sonde de capacité "mise à jour silencieuse"
     *  (cf. onPageFinished ci-dessus) -- custom.js le rapporte à Supabase par
     *  mosque_id, pour affichage dans l'administration à distance. */
    private fun dispatchSilentUpdateCapability(capable: Boolean) {
        if (!isPageLoaded) return
        val model = android.os.Build.MODEL.replace("'", "")
        val release = android.os.Build.VERSION.RELEASE.replace("'", "")
        webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('ucSilentUpdateCapability'," +
                "{detail:{capable:$capable,model:'$model',androidVersion:'$release'}}));",
            null
        )
        Log.d("TWKT", "Dispatched ucSilentUpdateCapability: capable=$capable")
    }

    /** Lance RemoteSilentUpdater.run() en tâche de fond (cf. MobileJsBridge
     *  .startSilentAppUpdate, action 'update_app') puis rapporte le résultat
     *  au WebView. Progression (dialogue natif + rapport Supabase, demandé
     *  le 31/07/2026) transmise via silentUpdateOnProgress -- la box reste
     *  sans surveillance, mais un admin present devant l'ecran ou consultant
     *  a distance depuis son telephone peut suivre l'avancement. */
    private fun runRemoteSilentUpdate() {
        Log.d("TWKT", "Remote silent update: starting")
        CoroutineScope(Dispatchers.IO).launch {
            val outcome = RemoteSilentUpdater.run(this@MainActivity) { p ->
                runOnUiThread { silentUpdateOnProgress(p) }
            }
            Log.d("TWKT", "Remote silent update outcome: $outcome")
            withContext(Dispatchers.Main) { dispatchSilentUpdateOutcome(outcome) }
        }
    }

    /**
     * Active/désactive la boucle de vérification quotidienne (heure choisie
     * par l'admin, cf. hour/minute) de mise à jour silencieuse -- remplace
     * le sondage push/60s comme mécanisme de production (cf. discussion
     * 31/07/2026 : le push OneSignal s'est révélé peu fiable sur cette
     * classe de boîtier, best-effort par nature). Piloté depuis custom.js
     * (ucMosqueInfoAdminSection, JS_CUSTOM.ucAutoDailyUpdateEnabled/
     * ucAutoDailyUpdateTime), appelé à chaque changement (case à cocher OU
     * champ horaire) -- annule systématiquement toute boucle existante avant
     * d'en (re)lancer une avec les valeurs actuelles, pour qu'un changement
     * d'heure pendant que c'est déjà activé prenne effet immédiatement.
     */
    fun setAutoDailyUpdateEnabled(enabled: Boolean, hour: Int, minute: Int) {
        if (!DeviceType.isAndroidTv(this)) return
        dailySilentUpdateJob?.cancel()
        dailySilentUpdateJob = null
        if (!enabled) {
            Log.d("TWKT", "Daily silent update check: disabled")
            return
        }
        Log.d("TWKT", "Daily silent update check: enabled at %02d:%02d".format(hour, minute))
        dailySilentUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(delayUntilNextTimeMs(hour, minute))
                runDailySilentUpdateCheck("Daily")
            }
        }
    }

    private fun delayUntilNextTimeMs(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis - System.currentTimeMillis()
    }

    private suspend fun runDailySilentUpdateCheck(label: String) {
        Log.d("TWKT", "$label silent update check: starting")
        val outcome = RemoteSilentUpdater.run(this@MainActivity) { p ->
            runOnUiThread { silentUpdateOnProgress(p) }
        }
        Log.d("TWKT", "$label silent update check outcome: $outcome")
        withContext(Dispatchers.Main) { dispatchSilentUpdateOutcome(outcome) }
    }

    /** Appelé (déjà sur le thread UI) à chaque étape de RemoteSilentUpdater.run()
     *  qui a effectivement quelque chose à montrer (donc jamais pour le cas
     *  "déjà à jour", qui ne passe pas par ce callback) : met à jour le
     *  dialogue visible sur la box ET rapporte la progression au WebView pour
     *  écriture Supabase (cf. custom.js, table mosque_device_status). */
    private fun silentUpdateOnProgress(progress: RemoteSilentUpdater.Progress) {
        SilentUpdateProgressDialog.update(this, progress)
        dispatchSilentUpdateProgress(progress)
    }

    private fun dispatchSilentUpdateProgress(progress: RemoteSilentUpdater.Progress) {
        if (!isPageLoaded) return
        val safePhase = progress.phase.replace("'", "\\'")
        val safeMsg = progress.message.replace("'", "\\'").replace("\n", " ")
        val pctJs = progress.pct?.toString() ?: "null"
        webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('ucSilentUpdateProgress'," +
                "{detail:{phase:'$safePhase',message:'$safeMsg',pct:$pctJs," +
                "bytesDownloaded:${progress.bytesDownloaded},totalBytes:${progress.totalBytes}}}));",
            null
        )
    }

    private fun dispatchSilentUpdateOutcome(outcome: RemoteSilentUpdater.Outcome) {
        if (!isPageLoaded) return
        val safeMsg = outcome.message.replace("'", "\\'").replace("\n", " ")
        webView.evaluateJavascript(
            "window.dispatchEvent(new CustomEvent('ucSilentUpdateResult'," +
                "{detail:{success:${outcome.success},silent:${outcome.silent},message:'$safeMsg'}}));",
            null
        )
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

    // Lit la langue courante côté JS (_ucLang(), custom.js) pour les boutons
    // OK/Annuler des dialogues alert()/confirm() natifs -- cf. onJsAlert/
    // onJsConfirm ci-dessous. evaluateJavascript() est asynchrone mais son
    // callback est déjà garanti sur le thread UI (contrat WebView standard),
    // donc l'AlertDialog peut être construite directement dedans.
    private fun fetchUcLangThen(then: (String) -> Unit) {
        webView.evaluateJavascript(
            "(function(){try{return (typeof _ucLang==='function')?_ucLang():'EN';}catch(e){return 'EN';}})()"
        ) { raw ->
            then(raw?.trim('"') ?: "EN")
        }
    }

    // AR/FR/EN uniquement : mêmes 3 langues garanties par tout dictionnaire de
    // custom.js (cf. le commentaire au-dessus de _ucLang() dans ce fichier) --
    // repli sur EN pour les 44 autres langues proposées côté appli.
    private fun dialogLabel(key: String, lang: String): String {
        val table = mapOf(
            "ok" to mapOf("AR" to "موافق", "FR" to "OK", "EN" to "OK"),
            "cancel" to mapOf("AR" to "إلغاء", "FR" to "Annuler", "EN" to "Cancel")
        )
        return table[key]?.get(lang) ?: table[key]?.get("EN") ?: key
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    private fun setupWebView() {
        // Test diagnostic (17/08/2026, retour utilisateur -- boîtier TV,
        // artefact visuel : petit carré/point vert qui apparaît près de
        // l'horloge et change de position/forme à chaque seconde, propre au
        // redessin fréquent de #countdownDisplayVertical). Piste retenue :
        // bug du compositeur GPU du pilote SoC bon marché (Allwinner/
        // Amlogic/Rockchip), PAS un bug applicatif -- ni permission CAMERA/
        // RECORD_AUDIO (aucune des deux déclarée, indicateur de vie privée
        // Android exclu), ni contenu vidéo/EME (aucun <video>/DRM dans les
        // assets), ni flag de debug de rendu (seul setWebContentsDebuggingEnabled
        // ci-dessous est actif, sans effet visuel). Même famille de bug déjà
        // rencontrée sur ce projet pour #quranPlayerOverlay (cf. custom.css,
        // compositeur GPU qui échoue à peindre un overlay position:fixed sur
        // certains boîtiers bon marché) -- ici LAYER_TYPE_SOFTWARE fait
        // l'inverse (retire le WebView du compositeur GPU au lieu de forcer
        // une couche dédiée) pour vérifier si ça élimine l'artefact.
        // Limité aux boîtiers TV (DeviceType.isAndroidTv) : sur téléphone,
        // le rendu matériel n'est pas en cause et reste préférable
        // (fluidité/consommation) -- à retirer si le test ne change rien,
        // ou à garder si confirmé en conditions réelles sur le boîtier.
        //
        // RESOLU (22/08/2026, retour utilisateur -- marquee saccade ~10 img/s
        // sur boitier TV, cf. TODO du 20/08/2026 ci-dessus dans l'historique
        // git) : on reprend la 2e piste envisagee plutot que de sacrifier le
        // rendu GPU de toute la page. LAYER_TYPE_SOFTWARE supprime, le
        // WebView reste en LAYER_TYPE_HARDWARE (comportement par defaut) sur
        // TOUS les appareils, y compris boitier TV -- le marquee et toutes
        // les autres animations CSS retrouvent le compositeur GPU. En
        // echange, l'element horloge/compteur suspecte d'origine (redessine
        // chaque seconde, cf. artefact vert du 17/08/2026) est isole sur sa
        // propre couche de composition cote CSS (transform:translateZ(0) +
        // backface-visibility:hidden sur #fullClockTimeVertical/Horizontal,
        // #miniClockTimeVertical/Horizontal, #secondCounterVertical/
        // Horizontal -- cf. custom.css, meme technique deja validee dans ce
        // projet pour #quranPlayerOverlay, meme famille de bug pilote GPU
        // boitier bon marche). Id exact de l'epoque (#countdownDisplayVertical,
        // TODO ci-dessus) introuvable dans les assets actuels au moment de ce
        // changement -- cible donc l'ensemble des elements horloge/compteur
        // mis a jour chaque seconde plutot qu'un seul id incertain.
        // A VERIFIER EN CONDITIONS REELLES sur boitier TV (l'artefact ne se
        // constate qu'a l'oeil, pas via les logs) : si le petit carre/point
        // vert revient malgre l'isolation CSS ci-dessus, revenir a
        // LAYER_TYPE_SOFTWARE (git revert) en attendant une autre piste.
        //
        // PRECISION (22/08/2026, retour utilisateur : bug constate UNIQUEMENT
        // sur boitier en mode HORIZONTAL) : #countdownDisplayVertical
        // (mentionne plus haut) est introuvable dans les assets et un bug
        // horizontal-only ne peut de toute facon pas venir d'un id
        // "...Vertical" -- coupable corrige dans custom.css :
        // #fullScreenCounterMiniClockHorizontal (mini horloge du compteur
        // plein ecran, HORIZONTAL UNIQUEMENT, aucun equivalent Vertical).

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
                onRemoteSilentUpdate = { runRemoteSilentUpdate() },
                onOpenTvUtilityMenu = {
                    runOnUiThread { showTvUtilityMenu() }
                },
                onSetAutoDailyUpdate = { enabled, hour, minute -> setAutoDailyUpdateEnabled(enabled, hour, minute) },
                onPickCustomAzanFile = { groupKey ->
                    pendingCustomAzanGroup = groupKey
                    pickAudioLauncher.launch(arrayOf("audio/mpeg", "audio/ogg", "audio/mp4", "audio/x-wav", "audio/*"))
                },
                onReportLoadProgress = { percent ->
                    runOnUiThread {
                        splashProgress.progress = percent
                        splashProgressText.text = "$percent%"
                    }
                },
                onAppFullyReady = {
                    runOnUiThread { hideSplash() }
                }
            ),
            "AndroidMobile"
        )

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.d("TWKT", "[${msg.messageLevel()}] ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")
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
            // Sans ces deux overrides, alert()/confirm() (custom.js) passent par
            // l'implémentation par défaut du WebView, qui affiche un titre
            // parasite "La page à l'adresse 'file://...' indique :" au-dessus du
            // message — retour utilisateur explicite (17/08/2026). On reprend le
            // message tel quel dans une AlertDialog sans titre ; les libellés des
            // boutons suivent la langue courante de l'appli (JS_DATA.ucLangNOW,
            // lue côté JS via _ucLang() — cf. custom.js) plutôt que la locale
            // système, pour rester cohérent avec le reste de l'UI.
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                fetchUcLangThen { lang ->
                    if (isFinishing || isDestroyed) { result.confirm(); return@fetchUcLangThen }
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(dialogLabel("ok", lang)) { _, _ -> result.confirm() }
                        .show()
                }
                return true
            }
            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                fetchUcLangThen { lang ->
                    if (isFinishing || isDestroyed) { result.cancel(); return@fetchUcLangThen }
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(dialogLabel("ok", lang)) { _, _ -> result.confirm() }
                        .setNegativeButton(dialogLabel("cancel", lang)) { _, _ -> result.cancel() }
                        .show()
                }
                return true
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
                // NE MASQUE PLUS le splash ici (retire le 19/08/2026) : bug
                // trouve en direct (logcat, boitier Mediouni) -- req.isForMainFrame
                // remonte vrai sur ce WebView (Chrome/91) pour de simples
                // ressources manquantes routinieres et attendues (ex. fichier
                // d'horaires par ville absent pour cette mosquee,
                // wtimes-tn.ksibet-el-mediouni_.js -> ERR_FILE_NOT_FOUND, gere
                // cote JS avec repli), pas seulement pour un vrai echec du
                // document principal. Ca revelait l'appli des la premiere
                // erreur benigne (~4% de progression constate), bien avant la
                // fin reelle du chargement. Le splash ne se ferme plus que via
                // MobileJsBridge.notifyAppFullyReady() (signal JS explicite de
                // fin de chargement) ou SPLASH_FALLBACK_TIMEOUT_MS (filet de
                // securite si index.html lui-meme ne charge jamais).
            }
            // Cold-start via tap notification : dispatch ucConfigSync après chargement complet
            // NE MASQUE PLUS l'ecran de chargement ici (retire le 19/08/2026) :
            // onPageFinished ne signale que la fin du parsing HTML/des scripts
            // synchrones, bien avant que custom.js ait fini de construire le
            // DOM/CSS final -- l'utilisateur voyait cette construction
            // progressive juste apres la disparition du splash (retour
            // explicite). hideSplash() n'est plus declenche que par
            // MobileJsBridge.notifyAppFullyReady() (cf. custom.js, tout en
            // bas du fichier) ou par le filet de securite SPLASH_FALLBACK_TIMEOUT_MS.
            override fun onPageFinished(view: WebView, url: String) {
                isPageLoaded = true
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
                // Sonde de capacité "mise à jour silencieuse" (box uniquement -- un
                // téléphone n'est jamais mis à jour à distance sans surveillance) :
                // pas de garde "une seule fois par process" contrairement à
                // automaticUpdateCheckStarted ci-dessus -- checkCapability() est un
                // test local (su 0 id), pas un appel réseau, donc pas besoin de
                // l'économiser ; le refaire à chaque rechargement (y compris via le
                // bouton "Recharger la box" de l'administration à distance) permet
                // de rafraîchir le statut sans action dédiée supplémentaire.
                if (DeviceType.isAndroidTv(this@MainActivity)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Device Owner (officiel, cf. DeviceOwnerInstaller) d'abord --
                        // simple appel DevicePolicyManager, quasi instantané ; la sonde
                        // su (checkCapability) n'est faite qu'en second, seulement si
                        // la box n'est pas Device Owner (quasi toujours false -- cf.
                        // commentaire de classe SilentUpdateHelper -- pas la peine de
                        // payer son coût de ~1-3s à chaque chargement si déjà capable).
                        val isDo = DeviceOwnerInstaller.isDeviceOwner(this@MainActivity)
                        if (isDo) TvHomeLauncherHelper.enforceDeviceOwnerHome(this@MainActivity)
                        val capable = isDo || SilentUpdateHelper.checkCapability()
                        withContext(Dispatchers.Main) { dispatchSilentUpdateCapability(capable) }
                    }
                    // La vérification automatique quotidienne (01:00) est démarrée/
                    // arrêtée explicitement par custom.js via setAutoDailyUpdateEnabled
                    // (ucMosqueInfoAdminSection), pas ici -- l'état persiste côté JS
                    // (JS_CUSTOM.ucAutoDailyUpdateEnabled), pas besoin de la relancer
                    // à chaque chargement de page côté natif.
                }
                maybeShowAutoStartSetupPrompt(view)
                maybeShowTvHomeLauncherPrompt(view)

                // Boitier (aucun humain devant l'ecran pour toucher/cliquer une fois) :
                // simule un geste utilisateur reel au niveau du systeme de saisie
                // Android, cf. dispatchSyntheticUnlockTap() -- sans ca, la politique
                // autoplay de Chromium (AudioContext suspendu tant qu'aucun geste
                // "trusted" n'a eu lieu, cf. custom.js _installAudioUnlockFallback/
                // _doAudioUnlock) restait bloquee apres chaque installation fraiche ou
                // mise a jour silencieuse jusqu'a ce qu'un administrateur touche
                // physiquement l'ecran -- reproduit le symptome "en attente de geste"
                // signale par l'utilisateur (10/08/2026).
                if (DeviceType.isAndroidTv(this@MainActivity)) {
                    view.postDelayed({ dispatchSyntheticUnlockTap(view) }, 1500)
                }
            }
        }
    }

    /**
     * Simule un veritable tap (ACTION_DOWN + ACTION_UP) directement via
     * View.dispatchTouchEvent(), le meme chemin que suit un toucher physique
     * de l'ecran -- contrairement a un evenement DOM synthetise cote JS
     * (`new Event('click')`, isTrusted=false, ignore par la politique
     * autoplay), un MotionEvent delivre ainsi traverse le vrai pipeline de
     * saisie Android et est vu par Chromium comme un geste utilisateur
     * legitime. Coin (1,1) initialement choisi comme "hors de toute zone
     * cliquable" ouvrait en realite le menu (bouton hamburger en haut a
     * gauche, retour utilisateur 10/08/2026) -- coin haut-droit utilise a la
     * place (x = largeur de la vue - 1). Choisir une coordonnee "vide" reste
     * fragile en soi (une modale ouverte au moment du tap peut occuper ce
     * pixel -- reproduit en direct le 10/08/2026 sur box .68 : azanCatalogOverlay
     * se fermait tout seul ~1.5s apres un rechargement de page si elle etait
     * ouverte a ce moment) -- cf. spec/custom.js _installSyntheticUnlockTapTarget,
     * qui garantit desormais un element dedie (z-index max, aucun handler) a
     * exactement ce coin, quoi qu'il y ait par-dessus le reste de la page.
     */
    private fun dispatchSyntheticUnlockTap(view: WebView) {
        try {
            val x = if (view.width > 1) (view.width - 1).toFloat() else 1f
            val y = 1f
            val downTime = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
            val up   = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0)
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
            Log.d("TWKT", "Synthetic unlock tap dispatched at ($x, $y)")
        } catch (e: Exception) {
            Log.e("TWKT", "dispatchSyntheticUnlockTap failed: ${e.message}")
        }
    }

    /**
     * Masque l'ecran de chargement natif (logo + basmala + cercle de
     * progression) affiche par-dessus le WebView. Declenche par
     * MobileJsBridge.notifyAppFullyReady() (custom.js signale la fin reelle
     * de la construction du DOM/CSS ET l'evenement 'load' de la fenetre, cf.
     * tout en bas de custom.js) -- le filet de securite
     * SPLASH_FALLBACK_TIMEOUT_MS peut aussi l'appeler (signal JS jamais recu).
     * Idempotent : ne joue le fondu qu'une seule fois.
     */
    private fun hideSplash() {
        if (splashHidden) return
        splashHidden = true
        Log.d("TWKT", "hideSplash() at uptimeMs=${SystemClock.uptimeMillis()}")
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
        if (TvHomeLauncherHelper.isCurrentlyDefaultHome(this)) {
            TvHomeLauncherPrefs.resetReassertFailCount(this)
            return
        }
        // Certains boitiers crashent instantanement sur l'ecran systeme de
        // choix d'accueil (bug AOSP, cf. TvHomeLauncherPrefs), ce qui sans
        // ce garde-fou renvoie aussitot le focus a onResume() et rouvre le
        // picker en boucle continue (l'appli parait alors "plantee").
        if (!TvHomeLauncherPrefs.canAttemptReassert(this)) return
        Log.d("TWKT", "TV home launcher preference lost — reopening picker")
        TvHomeLauncherPrefs.recordReassertAttempt(this)
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
