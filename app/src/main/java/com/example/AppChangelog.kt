package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.23"
    val LINES = listOf(
        "Pulse: hint 'HOLD TO PULSE' spostato direttamente sotto l'avatar.",
        "Pulse: effetto bottone 3D più realistico (scala + ombra animate).",
        "Dialog: scroll interno non interferisce più con la chiusura swipe."
    )
}
