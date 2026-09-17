package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.15.7"
    val LINES = listOf(
        "Chat: scorrimento interno funziona normalmente; overscroll deliberato ai bordi chiude la dialog.",
        "Dialog: swipe per chiudere su lista vuota/corta ora funziona (chat, notifiche, ricerca, impostazioni).",
        "Impostazioni accessibili direttamente da Live e Feed.",
        "Spotify Premium: rilevamento brano istantaneo via listener, zero polling.",
    )
}
