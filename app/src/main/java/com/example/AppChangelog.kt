package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.35"
    val LINES = listOf(
        "Feed: carosello auto-avanza ogni 3s, reset su swipe manuale.",
        "Feed: etichetta settimana senza riquadro, verde per la settimana corrente.",
        "Navigazione: transizione blur+crossfade tra tab Live e Feed.",
        "Font unificato su tutta la schermata feed."
    )
}
