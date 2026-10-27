package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.19.0"
    val LINES = listOf(
        "Spotify: rilevamento del brano in ascolto tramite l'app Spotify (App Remote) — funziona anche senza l'accesso alle notifiche.",
        "Più affidabile sui telefoni che bloccano l'accesso alle notifiche: resta comunque il fallback automatico.",
    )
}
