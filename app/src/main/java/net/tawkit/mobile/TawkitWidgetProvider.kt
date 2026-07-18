package net.tawkit.mobile

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

class TawkitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        } catch (e: Exception) {
            Log.e("TWKT_W", "onUpdate error: ${e.message}")
        }
        scheduleNextMinuteUpdate(context)
    }

    override fun onEnabled(context: Context) {
        scheduleNextMinuteUpdate(context)
    }

    override fun onDisabled(context: Context) {
        cancelMinuteUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MINUTE_UPDATE) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, TawkitWidgetProvider::class.java)
                )
                if (ids.isNotEmpty()) {
                    for (id in ids) updateWidget(context, manager, id)
                    scheduleNextMinuteUpdate(context)
                } else {
                    cancelMinuteUpdate(context)
                }
            } catch (e: Exception) {
                Log.e("TWKT_W", "onReceive error: ${e.message}")
            }
        }
    }

    companion object {

        const val PREFS_NAME           = "tawkit_widget_prefs"
        const val KEY_PRAYER_DATA      = "prayer_data"
        const val ACTION_MINUTE_UPDATE = "net.tawkit.mobile.WIDGET_MINUTE_UPDATE"
        private const val REQUEST_CODE_MINUTE = 9001

        private val NAME_IDS = intArrayOf(
            R.id.widgetNameFajr, R.id.widgetNameDohr, R.id.widgetNameAssr,
            R.id.widgetNameMgrb, R.id.widgetNameIsha
        )
        private val TIME_IDS = intArrayOf(
            R.id.widgetTimeFajr, R.id.widgetTimeDohr, R.id.widgetTimeAssr,
            R.id.widgetTimeMgrb, R.id.widgetTimeIsha
        )
        private val ACCENT_IDS = intArrayOf(
            R.id.widgetAccentFajr, R.id.widgetAccentDohr, R.id.widgetAccentAssr,
            R.id.widgetAccentMgrb, R.id.widgetAccentIsha
        )

        /**
         * Etat du cycle azan → doaa → iqama pour la prière la plus proche de
         * "maintenant", calculé uniquement à partir des données déjà connues
         * du widget (minutes de l'azan + délai d'iqama réel envoyé par JS,
         * cf. custom.js/_sendToWidget) — aucun appel réseau/JS supplémentaire.
         *
         * "elapsed"/"until" sont calculés modulo 1440 (minutes/jour) pour
         * gérer nativement le passage de minuit (ex. Fajr ~04h vu depuis 23h59).
         */
        private data class PhaseState(
            val phase: String,   // MAIN | BEFORE | AZAN | DUA | WAIT
            val idx: Int,        // index (0..4) de la prière concernée, -1 si MAIN
            val value1: Int = 0, // BEFORE: minutes restantes ; WAIT: minutes écoulées depuis l'azan
            val value2: Int = 0  // WAIT: minutes restantes avant l'iqama
        )

        private fun computePhase(prayers: JSONArray, nowMin: Int): PhaseState {
            for (i in 0 until prayers.length()) {
                val p = prayers.getJSONObject(i)
                val azanMin = p.getInt("minutes")
                val iqamaDelay = p.optInt("iqama", 10)

                var elapsed = (nowMin - azanMin) % 1440
                if (elapsed < 0) elapsed += 1440
                var until = (azanMin - nowMin) % 1440
                if (until < 0) until += 1440

                when {
                    until in 1..5 -> return PhaseState("BEFORE", i, until)
                    elapsed == 0 -> return PhaseState("AZAN", i)
                    elapsed in 1..2 -> return PhaseState("DUA", i)
                    iqamaDelay > 3 && elapsed in 3 until iqamaDelay ->
                        return PhaseState("WAIT", i, elapsed, iqamaDelay - elapsed)
                }
            }
            return PhaseState("MAIN", -1)
        }

        private fun defaultLabels(): JSONObject = JSONObject().apply {
            put("before", "بعد"); put("azanNow", "الآن أذان"); put("dua", "دعاء بعد الأذان")
            put("since", "مضى"); put("iqamaIn", "الإقامة بعد"); put("min", "د")
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_tawkit)

                // Clic → ouvre l'application
                val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launch != null) {
                    val pi = PendingIntent.getActivity(
                        context, 0, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widgetRoot, pi)
                }

                val raw = context
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_PRAYER_DATA, null)

                if (raw == null) {
                    // Pas encore de données — inviter l'utilisateur à ouvrir l'appli
                    views.setTextViewText(R.id.widgetMosqueName,    "توقيت")
                    views.setTextViewText(R.id.widgetHijriDate,     "—")
                    views.setTextViewText(R.id.widgetGregorianDate, "—")
                    views.setTextViewText(R.id.widgetSunrise,       "—")
                    views.setTextViewText(R.id.widgetNextPrayer,    "افتح التطبيق لتحميل البيانات")
                    manager.updateAppWidget(widgetId, views)
                    return
                }

                val data    = JSONObject(raw)
                val prayers = data.getJSONArray("prayers")
                val now     = Calendar.getInstance()
                val nowMin  = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                // Nom de la mosquée
                val mosqueName = data.optString("mosque", "").trim()
                views.setTextViewText(R.id.widgetMosqueName, mosqueName.ifEmpty { "—" })

                // Dates
                views.setTextViewText(R.id.widgetHijriDate,     data.optString("hijri",     "—"))
                views.setTextViewText(R.id.widgetGregorianDate, data.optString("gregorian", "—"))

                // Ligne 2 : chourouq + compte à rebours
                views.setTextViewText(R.id.widgetSunrise, "☀ " + data.optString("sunrise", "—"))

                var nextIdx  = -1
                var nextName = ""
                var nextMin  = 0
                for (i in 0 until prayers.length()) {
                    val p  = prayers.getJSONObject(i)
                    val pm = p.getInt("minutes")
                    if (pm > nowMin) { nextIdx = i; nextName = p.getString("name"); nextMin = pm; break }
                }
                if (nextIdx < 0 && prayers.length() > 0) {
                    val fajr = prayers.getJSONObject(0)
                    nextIdx  = 0
                    nextName = fajr.getString("name")
                    nextMin  = fajr.getInt("minutes") + 1440
                }
                val rem = nextMin - nowMin
                val remStr = if (rem >= 60) "${rem / 60}h ${rem % 60}m" else "${rem}m"

                // Cycle azan → doaa → iqama : remplace l'affichage normal
                // (compte à rebours/dates/chourouq) tant qu'une phase est active.
                val labels = data.optJSONObject("labels") ?: defaultLabels()
                val phase  = computePhase(prayers, nowMin)
                val highlightIdx: Int

                if (phase.phase == "MAIN") {
                    views.setViewVisibility(R.id.widgetHeaderNormal, View.VISIBLE)
                    views.setViewVisibility(R.id.widgetHeaderPhase, View.GONE)
                    views.setTextViewText(R.id.widgetNextPrayer, "⏱ $remStr ← $nextName")
                    highlightIdx = nextIdx
                } else {
                    views.setViewVisibility(R.id.widgetHeaderNormal, View.GONE)
                    views.setViewVisibility(R.id.widgetHeaderPhase, View.VISIBLE)
                    val pName = prayers.getJSONObject(phase.idx).getString("name")
                    val minUnit = labels.optString("min", "min")
                    when (phase.phase) {
                        "BEFORE" -> {
                            views.setTextViewText(R.id.widgetPhaseName, pName)
                            views.setTextViewText(R.id.widgetPhaseDetail,
                                "${labels.optString("before")} ${phase.value1} $minUnit")
                        }
                        "AZAN" -> {
                            views.setTextViewText(R.id.widgetPhaseName, labels.optString("azanNow"))
                            views.setTextViewText(R.id.widgetPhaseDetail, pName)
                        }
                        "DUA" -> {
                            views.setTextViewText(R.id.widgetPhaseName, labels.optString("dua"))
                            views.setTextViewText(R.id.widgetPhaseDetail, pName)
                        }
                        "WAIT" -> {
                            views.setTextViewText(R.id.widgetPhaseName,
                                "$pName — ${labels.optString("iqamaIn")} ${phase.value2}$minUnit")
                            views.setTextViewText(R.id.widgetPhaseDetail,
                                "${labels.optString("since")} ${phase.value1}$minUnit")
                        }
                    }
                    highlightIdx = phase.idx
                }

                // Lignes 3 & 4 : tableau des 5 prières + barre d'accent (remplace
                // l'ancien fond plein "carré", trop proche visuellement des
                // colonnes voisines — un simple repère de couleur + barre fine).
                val colorNextText = Color.parseColor("#ffd35a")
                val colorName     = Color.parseColor("#c8a86a")
                val colorTime     = Color.parseColor("#f5e6c8")

                val count = prayers.length().coerceAtMost(5)
                for (i in 0 until count) {
                    val p = prayers.getJSONObject(i)
                    views.setTextViewText(NAME_IDS[i], p.getString("name"))
                    views.setTextViewText(TIME_IDS[i], p.getString("time"))
                    if (i == highlightIdx) {
                        views.setTextColor(NAME_IDS[i], colorNextText)
                        views.setTextColor(TIME_IDS[i], colorNextText)
                        views.setViewVisibility(ACCENT_IDS[i], View.VISIBLE)
                    } else {
                        views.setTextColor(NAME_IDS[i], colorName)
                        views.setTextColor(TIME_IDS[i], colorTime)
                        views.setViewVisibility(ACCENT_IDS[i], View.INVISIBLE)
                    }
                }

                manager.updateAppWidget(widgetId, views)
                Log.d("TWKT_W", "Widget #$widgetId updated — phase: ${phase.phase}, next: $nextName in ${rem}m")

            } catch (e: JSONException) {
                Log.e("TWKT_W", "JSON error: ${e.message}")
                showErrorWidget(context, manager, widgetId, "خطأ في البيانات")
            } catch (e: Exception) {
                Log.e("TWKT_W", "updateWidget error: ${e.message}")
                showErrorWidget(context, manager, widgetId, "خطأ — أعد فتح التطبيق")
            }
        }

        private fun showErrorWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            msg: String
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_tawkit)
                views.setTextViewText(R.id.widgetHijriDate, msg)
                views.setTextViewText(R.id.widgetGregorianDate, "")
                views.setTextViewText(R.id.widgetSunrise, "")
                views.setTextViewText(R.id.widgetNextPrayer, "")
                manager.updateAppWidget(widgetId, views)
            } catch (_: Exception) { /* absorber */ }
        }

        fun scheduleNextMinuteUpdate(context: Context) {
            try {
                val intent = Intent(context, TawkitWidgetProvider::class.java).apply {
                    action = ACTION_MINUTE_UPDATE
                }
                val pi = PendingIntent.getBroadcast(
                    context, REQUEST_CODE_MINUTE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, 1)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                }
            } catch (e: Exception) {
                // SecurityException si SCHEDULE_EXACT_ALARM non accordé sur Android 12 :
                // le widget ne se rafraîchit plus à la minute mais ne plante pas.
                Log.w("TWKT_W", "scheduleNextMinuteUpdate failed (exact alarm denied?): ${e.message}")
            }
        }

        fun cancelMinuteUpdate(context: Context) {
            try {
                val intent = Intent(context, TawkitWidgetProvider::class.java).apply {
                    action = ACTION_MINUTE_UPDATE
                }
                val pi = PendingIntent.getBroadcast(
                    context, REQUEST_CODE_MINUTE, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pi?.let {
                    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(it)
                }
            } catch (e: Exception) {
                Log.w("TWKT_W", "cancelMinuteUpdate error: ${e.message}")
            }
        }

        fun triggerUpdate(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, TawkitWidgetProvider::class.java)
                )
                for (id in ids) updateWidget(context, manager, id)
            } catch (e: Exception) {
                Log.e("TWKT_W", "triggerUpdate error: ${e.message}")
            }
        }
    }
}
