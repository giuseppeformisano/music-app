package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.33"
    val LINES = listOf(
        "Dialog: gesto continuo che sfonda l'edge della lista non muove più la dialog.",
        "Dialog: la dialog si muove solo con un gesto nuovo (header o swipe separato)."
    )
}
