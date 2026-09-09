package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.15.2"
    val LINES = listOf(
        "Dialog: swipe ovunque chiude quando la lista è corta/vuota.",
        "Dialog con lista scrollabile: swipe sulla lista scrolla, solo l'header chiude.",
    )
}
