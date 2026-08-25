package com.example.data

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestore centralizzato per la mutua esclusione delle notifiche (Push vs Locali).
 * Persiste gli ID su SharedPreferences in modo che la mutua esclusione funzioni
 * anche se il processo viene riavviato dall'OS.
 */
object NotificationDeduplicator {

    private val inMemoryKeys = ConcurrentHashMap.newKeySet<String>()

    /**
     * @return true se è la PRIMA volta in assoluto che vediamo questa notifica (procedi).
     * @return false se è GIÀ stata mostrata (annulla ed evita duplicato).
     */
    fun shouldShow(context: Context, notificationKey: String): Boolean {
        if (notificationKey.isBlank()) return true

        // 1. Controllo rapido in memoria RAM
        if (!inMemoryKeys.add(notificationKey)) {
            return false
        }

        // 2. Controllo persistente su SharedPreferences per sopravvivere ai riavvii di processo
        try {
            val prefs = context.getSharedPreferences("notification_dedup_cache", Context.MODE_PRIVATE)
            val prefKey = "notif_$notificationKey"
            if (prefs.getBoolean(prefKey, false)) {
                return false
            }
            prefs.edit().putBoolean(prefKey, true).apply()
        } catch (e: Exception) {
            // Fallback sicuro se SharedPreferences fallisce
        }

        return true
    }

    fun clear(context: Context) {
        inMemoryKeys.clear()
        try {
            context.getSharedPreferences("notification_dedup_cache", Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) { }
    }
}
