package net.tawkit.mosqueadmin

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

// Petit outil perso : administrer les propositions de nouvelle mosquee
// stockees dans Supabase public.mosque_config_backups (cf.
// app/src/main/assets/spec/custom.js, _ucProposeNewMosque / _pushRemoteBackup)
// et realiser le "pont" manquant vers public.mosques (la table que l'app
// consulte reellement) -- approuver = creer/mettre a jour la ligne mosques
// a partir du blob de sauvegarde ; refuser/annuler = la supprimer.
class MainActivity : AppCompatActivity() {

    private val sbUrl = "https://tjmjmlzwzebocfdmifrg.supabase.co"
    private val sbKey = "sb_publishable_P9MMDcQw_mM4bLqCVCj_3A_tdTK5Tj4"

    private lateinit var listContainer: LinearLayout
    private lateinit var detailContainer: LinearLayout
    private lateinit var scrollList: ScrollView
    private lateinit var scrollDetail: ScrollView
    private lateinit var txtGlobalStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listContainer   = findViewById(R.id.listContainer)
        detailContainer = findViewById(R.id.detailContainer)
        scrollList      = findViewById(R.id.scrollList)
        scrollDetail    = findViewById(R.id.scrollDetail)
        txtGlobalStatus = findViewById(R.id.txtGlobalStatus)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadList() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { showList() }

        loadList()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (scrollDetail.visibility == View.VISIBLE) {
            showList()
        } else {
            super.onBackPressed()
        }
    }

    private fun showList() {
        scrollDetail.visibility = View.GONE
        scrollList.visibility   = View.VISIBLE
    }

    private fun showDetail() {
        scrollList.visibility   = View.GONE
        scrollDetail.visibility = View.VISIBLE
    }

    // ── Reseau (HttpURLConnection brut, sans dependance externe -- meme
    //    pattern que hijri-admin) ─────────────────────────────────────────
    private fun httpRequest(
        method: String,
        path: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        callback: (Int, String) -> Unit
    ) {
        Thread {
            var code = -1
            var responseText: String
            try {
                val url = URL(sbUrl + path)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.setRequestProperty("apikey", sbKey)
                conn.setRequestProperty("Authorization", "Bearer $sbKey")
                conn.setRequestProperty("Content-Type", "application/json")
                for ((k, v) in extraHeaders) conn.setRequestProperty(k, v)
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                if (body != null) {
                    conn.doOutput = true
                    val os: OutputStream = conn.outputStream
                    os.write(body.toByteArray(Charsets.UTF_8))
                    os.flush()
                    os.close()
                }
                code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                responseText = stream?.bufferedReader()?.readText() ?: ""
            } catch (e: Exception) {
                responseText = "Erreur reseau : ${e.message}"
            }
            val finalResponse = responseText
            val finalCode = code
            runOnUiThread { callback(finalCode, finalResponse) }
        }.start()
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun sha256Hex(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ── Liste ─────────────────────────────────────────────────────────────
    private fun loadList() {
        txtGlobalStatus.text = "Chargement..."
        listContainer.removeAllViews()
        httpRequest(
            "GET",
            "/rest/v1/mosque_config_backups?select=mosque_id,mosque_name,location_code,status,updated_at&order=status.asc,updated_at.desc"
        ) { code, body ->
            if (code in 200..299) {
                try {
                    val arr = JSONArray(body)
                    if (arr.length() == 0) {
                        txtGlobalStatus.text = "Aucune mosquee dans mosque_config_backups."
                    } else {
                        txtGlobalStatus.text = "${arr.length()} mosquee(s)."
                        for (i in 0 until arr.length()) {
                            addListCard(arr.getJSONObject(i))
                        }
                    }
                } catch (e: Exception) {
                    txtGlobalStatus.text = "Erreur de lecture : ${e.message}"
                }
            } else {
                txtGlobalStatus.text = "Erreur chargement ($code) : $body"
            }
        }
    }

    private fun statusLabel(s: String) = when (s) {
        "approved" -> "✓ Approuvee"
        "rejected" -> "✗ Refusee"
        else -> "⏳ En attente"
    }

    private fun statusColor(s: String) = when (s) {
        "approved" -> Color.parseColor("#4caf50")
        "rejected" -> Color.parseColor("#e05555")
        else -> Color.parseColor("#c8a84b")
    }

    private fun addListCard(o: JSONObject) {
        val mosqueId = o.optString("mosque_id", "")
        val name     = o.optString("mosque_name", mosqueId)
        val loc      = o.optString("location_code", "?")
        val status   = o.optString("status", "pending")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.parseColor("#242440"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 14) }
            isClickable = true
            isFocusable = true
        }
        card.addView(TextView(this).apply {
            text = name
            setTextColor(Color.WHITE)
            textSize = 16f
        })
        card.addView(TextView(this).apply {
            text = "$mosqueId  ·  $loc"
            setTextColor(Color.parseColor("#999999"))
            textSize = 12f
        })
        card.addView(TextView(this).apply {
            text = statusLabel(status)
            setTextColor(statusColor(status))
            textSize = 12f
            setPadding(0, 6, 0, 0)
        })
        card.setOnClickListener { loadDetail(mosqueId) }
        listContainer.addView(card)
    }

    // ── Detail ────────────────────────────────────────────────────────────
    private fun loadDetail(mosqueId: String) {
        txtGlobalStatus.text = "Chargement du detail..."
        httpRequest("GET", "/rest/v1/mosque_config_backups?mosque_id=eq.${enc(mosqueId)}&select=*") { code, body ->
            if (code in 200..299) {
                try {
                    val arr = JSONArray(body)
                    if (arr.length() == 0) {
                        txtGlobalStatus.text = "Introuvable."
                        return@httpRequest
                    }
                    txtGlobalStatus.text = ""
                    buildDetail(arr.getJSONObject(0))
                    showDetail()
                } catch (e: Exception) {
                    txtGlobalStatus.text = "Erreur : ${e.message}"
                }
            } else {
                txtGlobalStatus.text = "Erreur ($code) : $body"
            }
        }
    }

    private fun addDetailTitle(text: String) {
        detailContainer.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#c8a84b"))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 22, 0, 6)
        })
    }

    private fun addDetailRow(label: String, value: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#999999"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = value
            setTextColor(Color.WHITE)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        detailContainer.addView(row)
    }

    private fun buildDetail(row: JSONObject) {
        detailContainer.removeAllViews()

        val mosqueId = row.optString("mosque_id", "")
        val name     = row.optString("mosque_name", "")
        val loc      = row.optString("location_code", "")
        val status   = row.optString("status", "pending")
        val updated  = row.optString("updated_at", "")

        val backupJson = row.optJSONObject("backup_json")
        val dataObj = backupJson?.optJSONObject("data")
        val jsData = try {
            dataObj?.optString("JS_DATA")?.takeIf { it.isNotEmpty() }?.let { JSONObject(it) } ?: JSONObject()
        } catch (e: Exception) { JSONObject() }
        val jsCustom = try {
            dataObj?.optString("JS_DATA_CUSTOM")?.takeIf { it.isNotEmpty() }?.let { JSONObject(it) } ?: JSONObject()
        } catch (e: Exception) { JSONObject() }

        addDetailTitle(name.ifEmpty { mosqueId })
        addDetailRow("ID", mosqueId)
        addDetailRow("Ville (code)", loc)
        addDetailRow("Statut", statusLabel(status))
        if (updated.isNotEmpty()) addDetailRow("Derniere maj", updated.take(19).replace("T", " "))

        addDetailTitle("Azan (offset, min)")
        addDetailRow("Fajr / Shrouq", "${jsData.optInt("ucAthanMinutesFAJR", 0)} / ${jsData.optInt("ucAthanMinutesSHRQ", 0)}")
        addDetailRow("Dhuhr / Asr", "${jsData.optInt("ucAthanMinutesDOHR", 0)} / ${jsData.optInt("ucAthanMinutesASSR", 0)}")
        addDetailRow("Maghrib / Isha", "${jsData.optInt("ucAthanMinutesMGRB", 0)} / ${jsData.optInt("ucAthanMinutesISHA", 0)}")

        addDetailTitle("Iqama (delai, min)")
        addDetailRow("Fajr / Shrouq", "${jsData.optInt("ucIqamaFAJR", 0)} / ${jsData.optInt("ucIqamaSHRQ", 0)}")
        addDetailRow("Dhuhr / Asr", "${jsData.optInt("ucIqamaDOHR", 0)} / ${jsData.optInt("ucIqamaASSR", 0)}")
        addDetailRow("Maghrib / Isha", "${jsData.optInt("ucIqamaMGRB", 0)} / ${jsData.optInt("ucIqamaISHA", 0)}")
        val fixedFajr = jsData.optString("ucFixedIqamaFAJR", "")
        val fixedDohr = jsData.optString("ucFixedIqamaDOHR", "")
        val fixedAsr  = jsData.optString("ucFixedIqamaASSR", "")
        val fixedIsha = jsData.optString("ucFixedIqamaISHA", "")
        if (fixedFajr.isNotEmpty() || fixedDohr.isNotEmpty() || fixedAsr.isNotEmpty() || fixedIsha.isNotEmpty()) {
            addDetailRow("Iqama fixe (F/D/A/I)", "$fixedFajr / $fixedDohr / $fixedAsr / $fixedIsha")
        }

        addDetailTitle("Jumu'a / Eid")
        addDetailRow("Jumu'a activee", if (jsData.optInt("ucJomoaOnHRscreen", 0) == 1) "Oui" else "Non")
        addDetailRow("Heure Jumu'a", jsData.optString("ucJomoaFixedTime", "AUTO").ifEmpty { "AUTO" })
        addDetailRow("Eid Fitr", if (jsData.optInt("ucActivateEidFITR", 0) == 1) jsData.optString("ucTimeOfEidFITR", "") else "desactive")
        addDetailRow("Eid Adha", if (jsData.optInt("ucActivateEidADHA", 0) == 1) jsData.optString("ucTimeOfEidADHA", "") else "desactive")

        addDetailTitle("Autres")
        addDetailRow("Azan principal (min)", jsData.optInt("ucPrimaryAzanMinutes", 0).toString())
        addDetailRow("Dohr avant Asr (min)", jsData.optInt("ucDohrXminutesAsr", 0).toString())
        val pin = jsCustom.optString("ucAdminPin", "")
        addDetailRow("PIN admin", if (pin.isNotEmpty()) "configure (chiffre a l'approbation)" else "non configure")

        val quranEnabled = jsCustom.optInt("ucStartQuranBeforeAzan", 0) == 1
        addDetailRow("Coran avant azan", if (quranEnabled) "active" else "desactive")

        // ── Profil / coordonnees (JS_DATA_CUSTOM, cf. custom.js
        //    _installMosqueInfoModal -- ucMPE* dans _finishSelectMosque) :
        //    purement informatif, ces champs n'ont pas de colonne dans
        //    public.mosques (aucune n'existe pour l'instant) -- affiches ici
        //    pour revue admin, jamais envoyes lors de l'approbation.
        addDetailTitle("Profil de la mosquee")
        val address = jsCustom.optString("ucMosqueAddress", "")
        addDetailRow("Adresse", address.ifEmpty { "-" })
        addDetailRow("Telephone", jsCustom.optString("ucMosquePhone", "").ifEmpty { "-" })
        addDetailRow("Email", jsCustom.optString("ucMosqueEmail", "").ifEmpty { "-" })
        val pLat = jsCustom.optString("ucMosqueLat", "")
        val pLng = jsCustom.optString("ucMosqueLng", "")
        addDetailRow("GPS", if (pLat.isNotEmpty() && pLng.isNotEmpty()) "$pLat, $pLng" else "-")
        addDetailRow("Espace femmes", if (jsCustom.optInt("ucMosqueWomenAllowed", 0) == 1) "Oui" else "Non")
        addDetailRow("Ablution femmes", if (jsCustom.optInt("ucMosqueWomenAblution", 0) == 1) "Oui" else "Non")
        addDetailRow("Salat Janaza", if (jsCustom.optInt("ucMosqueJanaza", 0) == 1) "Oui" else "Non")
        addDetailRow("Kottab", if (jsCustom.optInt("ucMosqueKottab", 0) == 1) "Oui" else "Non")
        addDetailRow("Parking", if (jsCustom.optInt("ucMosqueParking", 0) == 1) "Oui" else "Non")
        val social = jsCustom.optString("ucMosqueSocialUrl", "")
        if (social.isNotEmpty()) addDetailRow("Reseau social", social)
        val photo = jsCustom.optString("ucMosqueImageUrl", "")
        if (photo.isNotEmpty()) addDetailRow("Photo", photo)

        // ── Actions selon statut ─────────────────────────────────────────
        addDetailTitle("Actions")
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 6, 0, 0)
        }
        detailContainer.addView(actionsRow)

        if (status == "approved") {
            val btnCancel = Button(this).apply { text = "Annuler l'approbation" }
            actionsRow.addView(btnCancel)
            btnCancel.setOnClickListener {
                confirmDialog(
                    "Annuler l'approbation ?",
                    "$name sera retiree de mosques ET de mosque_config_backups."
                ) { cancelApproval(mosqueId) }
            }
        } else {
            val btnApprove = Button(this).apply { text = "Approuver" }
            val btnReject  = Button(this).apply { text = "Refuser" }
            actionsRow.addView(btnApprove)
            actionsRow.addView(btnReject)
            btnApprove.setOnClickListener { showApproveForm(row, jsData, jsCustom) }
            btnReject.setOnClickListener {
                confirmDialog(
                    "Refuser cette proposition ?",
                    "$name sera supprimee de mosque_config_backups."
                ) { rejectProposal(mosqueId) }
            }
        }
    }

    private fun confirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirmer") { _, _ -> onConfirm() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ── Formulaire d'approbation (4 champs absents du backup) ──────────────
    private fun showApproveForm(row: JSONObject, jsData: JSONObject, jsCustom: JSONObject) {
        // Evite d'empiler le formulaire si deja affiche.
        if (detailContainer.findViewWithTag<View>("approveForm") != null) return

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "approveForm"
            setPadding(0, 10, 0, 0)
        }

        fun makeInput(hint: String, inputType: Int? = null): EditText = EditText(this).apply {
            this.hint = hint
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#888888"))
            if (inputType != null) this.inputType = inputType
        }

        val edCity    = makeInput("Ville affichee (ex: Ksibet Al-Mediouni)")
        val edCountry = makeInput("Pays (ex: TN)")
        val edLat     = makeInput("Latitude (optionnel)", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
        val edLon     = makeInput("Longitude (optionnel)", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
        // Pre-remplissage depuis le profil de la mosquee (ucMosqueLat/Lng,
        // saisis par l'imam dans Tawkit) -- evite une ressaisie manuelle,
        // l'admin peut toujours corriger avant de confirmer.
        jsCustom.optString("ucMosqueLat", "").takeIf { it.isNotEmpty() }?.let { edLat.setText(it) }
        jsCustom.optString("ucMosqueLng", "").takeIf { it.isNotEmpty() }?.let { edLon.setText(it) }
        // Pre-remplissage ville/pays depuis location_code (format
        // "<pays2>.<slug-ville>", ex. "tn.ksar-hellal") : le pays est
        // TOUJOURS deductible (prefixe), la ville est une suggestion a
        // partir du slug (peut differer du vrai nom affiche -- l'admin
        // corrige au besoin, ex. accents/nom complet).
        val locParts = row.optString("location_code", "").split(".", limit = 2)
        if (locParts.size == 2) {
            edCountry.setText(locParts[0].uppercase())
            edCity.setText(
                locParts[1].replace('-', ' ').replace('_', ' ').trim()
                    .split(" ").filter { it.isNotEmpty() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            )
        }

        form.addView(edCity)
        form.addView(edCountry)
        form.addView(edLat)
        form.addView(edLon)

        val txtFormStatus = TextView(this).apply { setPadding(0, 8, 0, 4) }
        form.addView(txtFormStatus)

        val btnConfirm = Button(this).apply { text = "Confirmer l'approbation" }
        form.addView(btnConfirm)

        btnConfirm.setOnClickListener {
            val city = edCity.text.toString().trim()
            val country = edCountry.text.toString().trim().uppercase()
            if (city.isEmpty() || country.isEmpty()) {
                txtFormStatus.setTextColor(Color.parseColor("#e05555"))
                txtFormStatus.text = "Ville et pays sont obligatoires."
                return@setOnClickListener
            }
            val lat = edLat.text.toString().trim().toDoubleOrNull()
            val lon = edLon.text.toString().trim().toDoubleOrNull()
            txtFormStatus.setTextColor(Color.parseColor("#c8a84b"))
            txtFormStatus.text = "Approbation en cours..."
            btnConfirm.isEnabled = false
            approveMosque(row, jsData, jsCustom, city, country, lat, lon) { ok, msg ->
                if (ok) {
                    txtFormStatus.setTextColor(Color.parseColor("#4caf50"))
                    txtFormStatus.text = "Approuvee. Retour a la liste..."
                    detailContainer.postDelayed({ showList(); loadList() }, 700)
                } else {
                    btnConfirm.isEnabled = true
                    txtFormStatus.setTextColor(Color.parseColor("#e05555"))
                    txtFormStatus.text = msg
                }
            }
        }

        detailContainer.addView(form)
    }

    // ── Construction du payload public.mosques (meme mapping que
    //    custom.js _buildAdminPayload / _buildQuranSettingsPayload) ────────
    private fun buildMosquesPayload(
        row: JSONObject, jsData: JSONObject, jsCustom: JSONObject,
        city: String, country: String, lat: Double?, lon: Double?
    ): JSONObject {
        val timeFlags = JSONObject()
        timeFlags.put("ramadan_isha_30min", jsData.optInt("ucRamadanDoIsha30min", 0))
        timeFlags.put("summer_time", jsData.optInt("ucInSummerAdd1Hour", 0))
        timeFlags.put("force_1h_more", jsData.optInt("ucForce1HourMore", 0))
        timeFlags.put("force_1h_less", jsData.optInt("ucForce1HourLess", 0))

        val azanOffsets = JSONObject()
        azanOffsets.put("fajr", jsData.optInt("ucAthanMinutesFAJR", 0))
        azanOffsets.put("shrouq", jsData.optInt("ucAthanMinutesSHRQ", 0))
        azanOffsets.put("dhuhr", jsData.optInt("ucAthanMinutesDOHR", 0))
        azanOffsets.put("asr", jsData.optInt("ucAthanMinutesASSR", 0))
        azanOffsets.put("maghrib", jsData.optInt("ucAthanMinutesMGRB", 0))
        azanOffsets.put("isha", jsData.optInt("ucAthanMinutesISHA", 0))

        val iqamaDelay = JSONObject()
        iqamaDelay.put("fajr", jsData.optInt("ucIqamaFAJR", 0))
        iqamaDelay.put("shrouq", jsData.optInt("ucIqamaSHRQ", 0))
        iqamaDelay.put("dhuhr", jsData.optInt("ucIqamaDOHR", 0))
        iqamaDelay.put("asr", jsData.optInt("ucIqamaASSR", 0))
        iqamaDelay.put("maghrib", jsData.optInt("ucIqamaMGRB", 0))
        iqamaDelay.put("isha", jsData.optInt("ucIqamaISHA", 0))

        val iqamaFixed = JSONObject()
        iqamaFixed.put("fajr", jsData.optString("ucFixedIqamaFAJR", ""))
        iqamaFixed.put("dhuhr", jsData.optString("ucFixedIqamaDOHR", ""))
        iqamaFixed.put("asr", jsData.optString("ucFixedIqamaASSR", ""))
        iqamaFixed.put("isha", jsData.optString("ucFixedIqamaISHA", ""))

        val jumua = JSONObject()
        jumua.put("time", jsData.optString("ucJomoaFixedTime", "AUTO").ifEmpty { "AUTO" })
        jumua.put("show_on_hr_screen", jsData.optInt("ucJomoaOnHRscreen", 0))

        val eid = JSONObject()
        eid.put("fitr_enabled", jsData.optInt("ucActivateEidFITR", 0))
        eid.put("fitr_time", jsData.optString("ucTimeOfEidFITR", "").trim())
        eid.put("adha_enabled", jsData.optInt("ucActivateEidADHA", 0))
        eid.put("adha_time", jsData.optString("ucTimeOfEidADHA", "").trim())

        var reciterDir = ""
        val reciters = jsCustom.optJSONArray("ucReciters")
        if (reciters != null) {
            for (i in 0 until reciters.length()) {
                val r = reciters.optJSONObject(i) ?: continue
                if (r.optInt("enabled", 0) == 1) { reciterDir = r.optString("dir", ""); break }
            }
        }
        val prayers = JSONObject()
        val prayerKeys = listOf("FAJR" to "Fajr", "DOHR" to "Dohr", "ASSR" to "Asr", "MGRB" to "Mgrb", "ISHA" to "Isha")
        for ((outKey, inKey) in prayerKeys) {
            val p = JSONObject()
            p.put("delay", jsCustom.optInt("ucStartQuranBeforeAzan$inKey", 0))
            p.put("days", jsCustom.optString("ucStartQuranBeforeAzan${inKey}Days", "1111111"))
            prayers.put(outKey, p)
        }
        val quranSettings = JSONObject()
        quranSettings.put("enabled", jsCustom.optInt("ucStartQuranBeforeAzan", 0))
        quranSettings.put("prayers", prayers)
        quranSettings.put("reciterDir", reciterDir)

        val pin = jsCustom.optString("ucAdminPin", "")

        val payload = JSONObject()
        payload.put("mosque_id", row.optString("mosque_id", ""))
        payload.put("mosque_name", row.optString("mosque_name", ""))
        payload.put("city", city)
        payload.put("country", country)
        payload.put("location_code", row.optString("location_code", ""))
        payload.put("status", "approved")
        payload.put("time_flags", timeFlags)
        payload.put("azan_offsets", azanOffsets)
        payload.put("iqama_delay", iqamaDelay)
        payload.put("iqama_fixed", iqamaFixed)
        payload.put("jumua", jumua)
        payload.put("eid", eid)
        payload.put("primary_azan", jsData.optInt("ucPrimaryAzanMinutes", 0))
        payload.put("dohr_before_asr_min", jsData.optInt("ucDohrXminutesAsr", 0))
        payload.put("quran_settings", quranSettings)
        if (lat != null) payload.put("latitude", lat)
        if (lon != null) payload.put("longitude", lon)
        if (pin.isNotEmpty()) payload.put("pin_hash", sha256Hex(pin))

        return payload
    }

    // ── Approuver : ecrit/upsert public.mosques puis passe le backup a
    //    status=approved ─────────────────────────────────────────────────
    private fun approveMosque(
        row: JSONObject, jsData: JSONObject, jsCustom: JSONObject,
        city: String, country: String, lat: Double?, lon: Double?,
        onDone: (Boolean, String) -> Unit
    ) {
        val mosqueId = row.optString("mosque_id", "")
        val payload = try {
            buildMosquesPayload(row, jsData, jsCustom, city, country, lat, lon)
        } catch (e: Exception) {
            onDone(false, "Erreur de construction : ${e.message}")
            return
        }
        httpRequest(
            "POST",
            "/rest/v1/mosques?on_conflict=mosque_id",
            JSONArray().put(payload).toString(),
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
        ) { code, body ->
            if (code in 200..299) {
                httpRequest(
                    "PATCH",
                    "/rest/v1/mosque_config_backups?mosque_id=eq.${enc(mosqueId)}",
                    JSONObject().put("status", "approved").toString(),
                    mapOf("Prefer" to "return=minimal")
                ) { code2, body2 ->
                    if (code2 in 200..299) onDone(true, "")
                    else onDone(false, "mosques ecrit, mais maj statut echouee ($code2) : $body2")
                }
            } else {
                onDone(false, "Erreur ecriture mosques ($code) : $body")
            }
        }
    }

    // ── Refuser (proposition en attente, jamais promue) ─────────────────────
    private fun rejectProposal(mosqueId: String) {
        txtGlobalStatus.text = "Suppression..."
        httpRequest(
            "DELETE",
            "/rest/v1/mosque_config_backups?mosque_id=eq.${enc(mosqueId)}",
            null,
            mapOf("Prefer" to "return=minimal")
        ) { code, body ->
            if (code in 200..299) {
                showList()
                loadList()
            } else {
                txtGlobalStatus.text = "Erreur suppression ($code) : $body"
            }
        }
    }

    // ── Annuler une approbation (retire mosques + mosque_config_backups) ──
    private fun cancelApproval(mosqueId: String) {
        txtGlobalStatus.text = "Annulation..."
        httpRequest(
            "DELETE",
            "/rest/v1/mosques?mosque_id=eq.${enc(mosqueId)}",
            null,
            mapOf("Prefer" to "return=minimal")
        ) { code, body ->
            if (code in 200..299) {
                httpRequest(
                    "DELETE",
                    "/rest/v1/mosque_config_backups?mosque_id=eq.${enc(mosqueId)}",
                    null,
                    mapOf("Prefer" to "return=minimal")
                ) { code2, body2 ->
                    if (code2 in 200..299) {
                        showList()
                        loadList()
                    } else {
                        txtGlobalStatus.text = "mosques supprime, mais backup non supprime ($code2) : $body2"
                    }
                }
            } else {
                txtGlobalStatus.text = "Erreur suppression mosques ($code) : $body"
            }
        }
    }
}
