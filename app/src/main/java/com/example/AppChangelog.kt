package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.36"
    val LINES = listOf(
        "Profilo: apertura e chiusura con blur+slide+crossfade.",
        "Dialog: blur progressivo sul contenuto durante lo swipe di chiusura.",
        "Feed: carosello auto-avanza ogni 3s, reset su swipe manuale."
    )
}
