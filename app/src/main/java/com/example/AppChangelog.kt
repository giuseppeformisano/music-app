package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.29"
    val LINES = listOf(
        "Dialog: transizioni swipe fluide a 120Hz con spring fisico e velocità.",
        "Dialog: fling veloce chiude immediatamente, snap-back con inerzia naturale.",
        "Dialog: resistenza rubber-band oltre soglia di trascinamento."
    )
}
