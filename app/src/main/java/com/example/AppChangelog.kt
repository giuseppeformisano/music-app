package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.25.4"
    val LINES = listOf(
        "App ora distribuita come build ottimizzata (release): avvio, animazioni e transizioni molto più reattivi, soprattutto al primo caricamento.",
        "Aggiornamento automatico senza reinstallare.",
    )
}
