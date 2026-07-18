package net.tawkit.mobile

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Coupe puis restaure la sonnerie autour de l'azan (onglet الإعدادات de la
 * modale info-mosquée — cf. custom.js, fonctions _ucScheduleSilentModeAlarms /
 * _ucToggleSilentBeforeAzan / _ucScheduleQuickMuteAfterAzanAlarms /
 * _ucToggleQuickMuteAfterAzan).
 *
 * Deux fonctionnalités indépendantes partagent ce receiver (deux alarmes
 * AlarmManager par prière chacune, ACTION_MUTE puis ACTION_RESTORE, même
 * mécanisme que PrayerAlarmReceiver — survit à la fermeture de
 * l'appli/WebView, contrairement à un simple setTimeout JS) :
 *   - REASON_BEFORE : coupe avant l'azan, remet après (fenêtre large, 1-180 min)
 *   - REASON_AFTER   : coupe pile à l'azan, remet après une courte durée (0-30 min)
 * Chaque intent porte un extra "reason" indiquant laquelle l'a programmée.
 *
 * Chevauchement (entre prières proches, OU entre les deux fonctionnalités si
 * les deux sont actives en même temps) : un compteur de fenêtres ouvertes PAR
 * REASON garantit qu'on ne restaure le son réel que lorsque TOUTES les
 * fenêtres (des deux reasons confondues) sont refermées, et qu'on ne capture
 * le mode d'origine qu'à l'ouverture de la toute première fenêtre globale —
 * les deux fonctionnalités s'additionnent donc naturellement (union) au lieu
 * de se marcher dessus l'une l'autre.
 */
class SilentModeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MUTE    = "net.tawkit.mobile.SILENT_MUTE"
        const val ACTION_RESTORE = "net.tawkit.mobile.SILENT_RESTORE"

        const val REASON_BEFORE = "before"
        const val REASON_AFTER  = "after"

        private const val PREFS_NAME      = "tawkit_silent_mode"
        private const val PREFS_PREV_MODE = "prev_ringer_mode"
        private const val MUTE_AFTER_NOTIFICATION_DELAY_MS = 1_500L

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private fun openCountKey(reason: String) = "open_count_$reason"

        private fun openCount(sp: SharedPreferences, reason: String): Int =
            sp.getInt(openCountKey(reason), 0)

        private fun totalOpenCount(sp: SharedPreferences): Int =
            openCount(sp, REASON_BEFORE) + openCount(sp, REASON_AFTER)

        /** Accès "Ne pas déranger" requis depuis API 23 pour changer le ringer mode. */
        fun hasNotificationPolicyAccess(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return nm.isNotificationPolicyAccessGranted
        }

        /** true si l'appli a au moins une fenêtre de coupure du son actuellement ouverte (toute reason confondue). */
        fun isAppSilencing(context: Context): Boolean =
            totalOpenCount(prefs(context)) > 0

        /**
         * Referme les fenêtres ouvertes pour UNE seule reason et restaure
         * immédiatement la sonnerie SI l'autre reason n'a plus de fenêtre
         * ouverte non plus — appelé quand l'utilisateur désactive l'une des
         * deux fonctionnalités depuis l'onglet الإعدادات (cf.
         * MobileJsBridge.disableSilentMode() / disableQuickMuteAfterAzan()),
         * pour ne pas laisser le téléphone silencieux jusqu'à la prochaine
         * RESTORE programmée (qui vient d'être annulée), tout en respectant
         * une fenêtre encore ouverte par l'autre fonctionnalité.
         */
        fun forceRestoreNow(context: Context, reason: String) {
            val sp = prefs(context)
            sp.edit().putInt(openCountKey(reason), 0).apply()
            val stillOpen = totalOpenCount(sp)
            if (stillOpen == 0 && hasNotificationPolicyAccess(context)) {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val prevMode = sp.getInt(PREFS_PREV_MODE, AudioManager.RINGER_MODE_NORMAL)
                try {
                    am.ringerMode = prevMode
                    Log.d("TWKT", "SilentMode: forceRestoreNow($reason) -> $prevMode")
                } catch (e: SecurityException) {
                    Log.e("TWKT", "SilentMode: forceRestoreNow($reason) refusé — ${e.message}")
                }
            } else if (stillOpen > 0) {
                Log.d("TWKT", "SilentMode: forceRestoreNow($reason) — fenêtre(s) restante(s) sur l'autre reason, pas de restauration")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!hasNotificationPolicyAccess(context)) {
            Log.w("TWKT", "SilentModeReceiver: accès 'Ne pas déranger' non accordé, ignoré (${intent.action})")
            return
        }
        MobileJsBridge.createNotificationChannel(context)

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sp = prefs(context)
        val prayer = intent.getStringExtra("prayer") ?: "Salat"
        val reason = intent.getStringExtra("reason") ?: REASON_BEFORE

        when (intent.action) {
            ACTION_MUTE -> {
                val totalBefore = totalOpenCount(sp)
                if (totalBefore == 0) {
                    // Toute première fenêtre (des deux reasons confondues) : on
                    // capture le mode actuel avant de couper.
                    sp.edit().putInt(PREFS_PREV_MODE, am.ringerMode).apply()
                }
                val newReasonCount = openCount(sp, reason) + 1
                sp.edit().putInt(openCountKey(reason), newReasonCount).apply()

                // Émettre avant le passage en silencieux permet à Android de
                // jouer le bip de cette notification.
                showSilentNotification(context, prayer, isRestore = false)

                // Maintient le receiver en vie pendant ce bref délai, sans
                // bloquer son thread principal.
                val pendingResult = goAsync()
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        // La fonction a pu être désactivée pendant le délai.
                        if (totalOpenCount(sp) > 0) {
                            am.ringerMode = AudioManager.RINGER_MODE_SILENT
                            Log.d("TWKT", "SilentMode: MUTE ($reason, fenêtres ouvertes=$newReasonCount)")
                        }
                    } catch (e: SecurityException) {
                        Log.e("TWKT", "SilentMode: MUTE refusé — ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }, MUTE_AFTER_NOTIFICATION_DELAY_MS)
            }

            ACTION_RESTORE -> {
                val newReasonCount = (openCount(sp, reason) - 1).coerceAtLeast(0)
                sp.edit().putInt(openCountKey(reason), newReasonCount).apply()
                val stillOpen = totalOpenCount(sp)
                if (stillOpen == 0) {
                    val prevMode = sp.getInt(PREFS_PREV_MODE, AudioManager.RINGER_MODE_NORMAL)
                    try {
                        am.ringerMode = prevMode
                        Log.d("TWKT", "SilentMode: RESTORE -> $prevMode (toutes fenêtres fermées)")
                        showSilentNotification(context, prayer, isRestore = true)
                    } catch (e: SecurityException) {
                        Log.e("TWKT", "SilentMode: RESTORE refusé — ${e.message}")
                    }
                } else {
                    Log.d("TWKT", "SilentMode: RESTORE différé ($reason, fenêtres restantes=$stillOpen)")
                }
            }
        }
    }

    private fun showSilentNotification(context: Context, prayer: String, isRestore: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val arabicName = mapOf(
            "Fajr" to "الفجر",
            "Dhuhr" to "الظهر",
            "Asr" to "العصر",
            "Maghreb" to "المغرب",
            "Isha" to "العشاء",
            "Jumua" to "الجمعة"
        )[prayer] ?: prayer

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = "net.tawkit.mobile.OPEN_PRAYER"
            putExtra("prayer", prayer)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val requestCode = prayer.hashCode() xor if (isRestore) 0x525354 else 0x4D5554
        val pendingTap = PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isRestore) "Sonnerie réactivée" else "Téléphone mis en muet"
        val body = if (isRestore) {
            "La période silencieuse après $prayer — $arabicName est terminée."
        } else {
            "Mode silencieux activé avant $prayer — $arabicName."
        }

        val notification = NotificationCompat.Builder(context, MobileJsBridge.CHANNEL_ID)
            .setSmallIcon(
                if (isRestore) android.R.drawable.ic_lock_silent_mode_off
                else android.R.drawable.ic_lock_silent_mode
            )
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        // Les tags distincts permettent aux trois types de coexister.
        val tag = if (isRestore) "silent_restore" else "silent_mute"
        nm.notify(tag, prayer.hashCode(), notification)
    }
}