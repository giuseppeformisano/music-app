package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.8"
    val LINES = listOf(
        "Collega account e Modifica profilo ora hanno l'header uniforme come gli altri pannelli.",
        "Freccia indietro e titolo allineati nella stessa identica posizione in tutti i pannelli (chat e dettagli inclusi).",
    )
}
