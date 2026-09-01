package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.21"
    val LINES = listOf(
        "Pulse: durata massima estesa a 10 secondi.",
        "Pulse: anelli pulsanti attorno all'avatar e hint 'HOLD TO PULSE' nel dettaglio live.",
        "Pulse: onda voce scrolling vicino all'avatar durante la registrazione.",
        "Pulse: sincronizzazione vibrazione/audio migliorata."
    )
}
