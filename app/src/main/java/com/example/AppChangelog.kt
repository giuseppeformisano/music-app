package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.5"
    val LINES = listOf(
        "Anche Cerca persone ora ha l'header uniforme: freccia indietro e titolo allineati come gli altri pannelli.",
        "Corretto il tasto Segui: la richiesta ora parte davvero e il tasto non torna indietro da solo.",
    )
}
