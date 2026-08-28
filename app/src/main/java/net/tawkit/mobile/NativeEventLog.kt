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
 *
 * ── NOTE PERFORMANCE (28/08/2026) ─────────────────────────────────────────
 * Ce journal ne recoit QUE des jalons (alarme, azan, lumieres FIRE/SKIP,
 * pause/reprise...), jamais du detail par tick : volume reel ~300 entrees /
 * jour, avec quelques rafales de ~15 sur 2-3 s aux transitions de priere.
 *
 * Deux choix ici, tous deux sans impact perceptible sur un boitier :
 *  1) MAX_ENTRIES = 1200 (au lieu de 300) : ~110 Ko serialises. Beaucoup plus
 *     d'historique conserve a travers les redemarrages.
 *  2) commit() (synchrone, fsync) au lieu de apply() : garantit que la
 *     DERNIERE entree survit a une COUPURE DE COURANT brutale (apply() ne
 *     flushe qu'en differe -> perdu si le courant tombe juste apres). Cout :
 *     ~10-20 ms par entree, hors thread UI (appele depuis le thread JS-bridge
 *     de la WebView ou un BroadcastReceiver), quelques centaines de fois par
 *     jour -> negligeable.
 * Le cache memoire (cache) supprime le seul vrai cout O(n) de l'ancienne
 * version : re-parser ~110 Ko de JSON a CHAQUE appel. On ne parse plus qu'une
 * fois par process ; ensuite chaque log() = append + trim en memoire +
 * serialisation (~1-2 ms) + commit().
 */
object NativeEventLog {
    private const val PREFS_NAME = "tawkit_native_event_log"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 1200

    // Source de verite en memoire : evite de re-parser la totalite du JSON
    // stocke a chaque log(). Chargee une seule fois par process (lazy), sous
    // le meme verrou @Synchronized que log()/clear().
    private var cache: JSONArray? = null

    private fun cacheOf(context: Context): JSONArray {
        cache?.let { return it }
        val loaded = try {
            JSONArray(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_ENTRIES, "[]")
            )
        } catch (e: Exception) {
            JSONArray()
        }
        cache = loaded
        return loaded
    }

    @Synchronized
    fun log(context: Context, tag: String, text: String) {
        try {
            val arr = cacheOf(context)
            arr.put(
                JSONObject()
                    .put("ts", System.currentTimeMillis())
                    .put("tag", tag)
                    .put("text", text)
            )
            // Trim en place : ne conserver que les MAX_ENTRIES dernieres.
            while (arr.length() > MAX_ENTRIES) arr.remove(0)
            // commit() : ecriture synchrone + fsync -> l'entree survit a une
            // coupure de courant brutale (cf. NOTE PERFORMANCE en tete).
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_ENTRIES, arr.toString()).commit()
        } catch (e: Exception) {
            android.util.Log.e("TWKT", "NativeEventLog.log failed: ${e.message}")
        }
    }

    @Synchronized
    fun getAllAsJson(context: Context): String {
        return try {
            cacheOf(context).toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    @Synchronized
    fun clear(context: Context) {
        try {
            cache = JSONArray()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_ENTRIES, "[]").commit()
        } catch (e: Exception) {
        }
    }
}
