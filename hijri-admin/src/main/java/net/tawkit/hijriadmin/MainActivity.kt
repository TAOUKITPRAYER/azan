package net.tawkit.hijriadmin

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

// Petit outil perso : saisir/mettre a jour la table Supabase
// public.hijri_month_starts (cf. app/src/main/assets/spec/custom.js,
// section "SYNCHRONISATION DATE HIJRI"), utilisee par Tawkit comme source
// officielle par pays pour le debut de chaque mois hijri.
class MainActivity : AppCompatActivity() {

    private val sbUrl = "https://tjmjmlzwzebocfdmifrg.supabase.co"
    private val sbKey = "sb_publishable_P9MMDcQw_mM4bLqCVCj_3A_tdTK5Tj4"

    private val monthNames = arrayOf(
        "1 - Mouharram", "2 - Safar", "3 - Rabi' al-Awwal", "4 - Rabi' ath-Thani",
        "5 - Joumada al-Oula", "6 - Joumada ath-Thania", "7 - Rajab", "8 - Cha'ban",
        "9 - Ramadan", "10 - Chawwal", "11 - Dhou al-Qi'da", "12 - Dhou al-Hijja"
    )

    private lateinit var rowsContainer: LinearLayout
    private lateinit var txtGlobalStatus: TextView

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    data class RowData(
        val country: String = "",
        val hijriYear: Int = 1448,
        val hijriMonth: Int = 1,
        val startDate: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rowsContainer = findViewById(R.id.rowsContainer)
        txtGlobalStatus = findViewById(R.id.txtGlobalStatus)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadExistingRows() }
        findViewById<Button>(R.id.btnAddRow).setOnClickListener { addRow(null) }

        loadExistingRows()
    }

    // ── Reseau (HttpURLConnection brut, sans dependance externe) ───────────

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

    // ── Chargement des dernieres lignes connues (vue hijri_month_starts_latest) ──

    private fun loadExistingRows() {
        txtGlobalStatus.text = "Chargement..."
        rowsContainer.removeAllViews()
        httpRequest("GET", "/rest/v1/hijri_month_starts_latest?select=*&order=country.asc") { code, body ->
            if (code in 200..299) {
                try {
                    val arr = JSONArray(body)
                    if (arr.length() == 0) {
                        txtGlobalStatus.text = "Aucune ligne existante. Ajoutez un pays ci-dessous."
                    } else {
                        txtGlobalStatus.text = "${arr.length()} pays charges depuis Supabase."
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            addRow(
                                RowData(
                                    country = o.getString("country"),
                                    hijriYear = o.getInt("hijri_year"),
                                    hijriMonth = o.getInt("hijri_month"),
                                    startDate = o.getString("gregorian_start_date")
                                )
                            )
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

    // ── Construction d'une ligne (carte pays) ───────────────────────────────

    private fun addRow(existing: RowData?) {
        val data = existing ?: RowData()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#242440"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
        }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val edCountry = EditText(this).apply {
            hint = "Pays (ex: TN)"
            setText(data.country)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val edYear = EditText(this).apply {
            hint = "Annee H."
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(data.hijriYear.toString())
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row1.addView(edCountry)
        row1.addView(edYear)
        card.addView(row1)

        val spMonth = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, monthNames)
            setSelection(data.hijriMonth - 1)
        }
        card.addView(spMonth)

        val btnNextMonth = Button(this).apply { text = "-> Mois suivant" }
        card.addView(btnNextMonth)

        var selectedDate = data.startDate
        val txtDate = TextView(this).apply {
            text = if (selectedDate.isNotEmpty()) "Debut : $selectedDate" else "Debut : (non defini)"
            setTextColor(Color.parseColor("#dddddd"))
            setPadding(0, 16, 0, 8)
        }
        card.addView(txtDate)

        val btnPickDate = Button(this).apply { text = "Choisir la date de debut" }
        card.addView(btnPickDate)

        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            if (selectedDate.isNotEmpty()) {
                try { cal.time = dateFmt.parse(selectedDate) ?: cal.time } catch (e: Exception) { }
            }
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = dateFmt.format(cal.time)
                txtDate.text = "Debut : $selectedDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnNextMonth.setOnClickListener {
            var m = spMonth.selectedItemPosition + 1
            var y = edYear.text.toString().toIntOrNull() ?: 1448
            m += 1
            if (m > 12) { m = 1; y += 1 }
            edYear.setText(y.toString())
            spMonth.setSelection(m - 1)
            selectedDate = ""
            txtDate.text = "Debut : (non defini)"
        }

        val txtStatus = TextView(this).apply {
            text = ""
            setPadding(0, 8, 0, 8)
        }
        card.addView(txtStatus)

        val row5 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnSave = Button(this).apply {
            text = "Enregistrer"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnRemove = Button(this).apply {
            text = "X"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        row5.addView(btnSave)
        row5.addView(btnRemove)
        card.addView(row5)

        btnRemove.setOnClickListener { rowsContainer.removeView(card) }

        btnSave.setOnClickListener {
            val country = edCountry.text.toString().trim().uppercase(Locale.US)
            val year = edYear.text.toString().toIntOrNull()
            val month = spMonth.selectedItemPosition + 1
            if (country.isEmpty() || year == null || selectedDate.isEmpty()) {
                txtStatus.setTextColor(Color.parseColor("#e05555"))
                txtStatus.text = "Pays, annee et date sont obligatoires."
                return@setOnClickListener
            }
            txtStatus.setTextColor(Color.parseColor("#c8a84b"))
            txtStatus.text = "Enregistrement..."

            val payload = JSONArray().put(
                JSONObject()
                    .put("country", country)
                    .put("hijri_year", year)
                    .put("hijri_month", month)
                    .put("gregorian_start_date", selectedDate)
            )

            httpRequest(
                "POST",
                "/rest/v1/hijri_month_starts?on_conflict=country,hijri_year,hijri_month",
                payload.toString(),
                mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
            ) { code, body ->
                if (code in 200..299) {
                    txtStatus.setTextColor(Color.parseColor("#4caf50"))
                    txtStatus.text = "Enregistre : $country $month/$year -> $selectedDate"
                } else {
                    txtStatus.setTextColor(Color.parseColor("#e05555"))
                    txtStatus.text = "Erreur ($code) : $body"
                }
            }
        }

        rowsContainer.addView(card)
    }
}
