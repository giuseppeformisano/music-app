package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.31"
    val LINES = listOf(
        "Dialog: swipe up/down sull'header (titolo o barra di ricerca) ora chiude davvero.",
        "Dialog: il tap sulla barra di ricerca continua a funzionare per scrivere.",
        "Dialog: le liste interne restano libere di scorrere senza chiudere per sbaglio."
    )
}
