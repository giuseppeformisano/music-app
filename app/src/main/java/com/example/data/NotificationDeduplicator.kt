package com.example.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Gestore centralizzato per la mutua esclusione delle notifiche (Push vs Locali).
 * Evita la duplicazione di qualsiasi tipo di notifica (richieste follow, messaggi, ecc.).
 */
object NotificationDeduplicator {

    private val shownNotificationKeys = ConcurrentHashMap.newKeySet<String>()

    /**
     * Tenta di registrare la notifica.
     * @return true se è la PRIMA volta che la notifica viene mostrata (procedi).
     * @return false se è GIÀ stata mostrata da un altro canale (annulla).
     */
    fun shouldShow(notificationKey: String): Boolean {
        if (notificationKey.isBlank()) return true
        return shownNotificationKeys.add(notificationKey)
    }

    /**
     * Resetta la cache delle notifiche mostrate (es. al logout).
     */
    fun clear() {
        shownNotificationKeys.clear()
    }
}
