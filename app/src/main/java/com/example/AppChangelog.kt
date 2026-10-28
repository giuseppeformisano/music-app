package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.25.0"
    val LINES = listOf(
        "Barra di navigazione (con tasto indietro) ora presente in TUTTI i pannelli.",
        "Tutti i pannelli entrano con la stessa transizione orizzontale (slide + blur) del profilo.",
    )
}
