package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.20.2"
    val LINES = listOf(
        "Chat e lista chat ora usano lo stesso rendering delle schermate normali: background corretto dietro la barra di stato.",
        "Tastiera: la barra di scrittura si solleva sopra la tastiera invece di finirci sotto.",
    )
}
