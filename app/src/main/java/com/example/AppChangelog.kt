package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.32"
    val LINES = listOf(
        "Dialog: il fling veloce sulla lista interna non sposta più la dialog per sbaglio.",
        "Dialog: solo il drag deliberato sull'edge della lista muove la dialog.",
        "Dialog: fling si ferma all'edge della lista, poi swipe separato muove la dialog."
    )
}
