package net.tawkit.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Journal persistant (SharedPreferences) du cycle de vie de l'azan
 * (programmation d'alarme, declenchement natif joue/saute, lecture webview,
 * passages premier-plan/arriere-plan). Necessaire car ces evenements peuvent
 * survenir appli fermee/arriere-plan, donc sans JS actif pour les capturer
 * dans la console de debug webview (_dbgLogs, custom.js) en temps reel.
 *
 * Consulte depuis custom.js via MobileJsBridge.getNativeEventLog(), qui
 * fusionne ces entrees dans la console de debug existante au chargement de
 * la page -- permet de reconstituer l'historique d'une journee complete
 * d'utilisation normale, y compris ce qui s'est passe appli fermee.
 */
object NativeEventLog {
    private const val PREFS_NAME = "tawkit_native_event_log"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 300

    @Synchronized
    fun log(context: Context, tag: String, text: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString(KEY_ENTRIES, "[]"))
            arr.put(
                JSONObject()
                    .put("ts", System.currentTimeMillis())
                    .put("tag", tag)
                    .put("text", text)
            )
            val trimmed = if (arr.length() > MAX_ENTRIES) {
                val start = arr.length() - MAX_ENTRIES
                val out = JSONArray()
                for (i in start until arr.length()) out.put(arr.get(i))
                out
            } else arr
            prefs.edit().putString(KEY_ENTRIES, trimmed.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("TWKT", "NativeEventLog.log failed: ${e.message}")
        }
    }

    fun getAllAsJson(context: Context): String {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ENTRIES, "[]") ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    fun clear(context: Context) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_ENTRIES, "[]").apply()
        } catch (e: Exception) {
        }
    }
}
