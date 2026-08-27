package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.12.0"
    val LINES = listOf(
        "Chat a finestra interattiva: la chat ora si chiude con lo swipe verso il basso da qualsiasi punto, senza pulsanti indietro.",
        "Bordi ultra-sottili e colori dinamici: ogni persona ha una palette contrastante dedicata (o estratta dalla copertina).",
        "Layout adattivo: il campo testo e i messaggi non traboccano mai sulla barra di stato e rimangono sempre sopra i pulsanti di sistema."
    )
}
