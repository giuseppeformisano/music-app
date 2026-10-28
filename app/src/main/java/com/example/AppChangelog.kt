package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.22.0"
    val LINES = listOf(
        "Barra di navigazione in basso anche in chat e lista chat, con tasto indietro per tornare alla schermata precedente.",
        "Chat: la barra di scrittura resta sopra la tastiera; i pulsanti stanno sotto.",
    )
}
