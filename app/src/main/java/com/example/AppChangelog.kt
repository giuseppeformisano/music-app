package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.14.4"
    val LINES = listOf(
        "Live: broadcaster e tutti i viewer vedono la stessa animazione cuori flottanti.",
        "Live: cuori flottanti si muovono anche orizzontalmente (drift ±18dp) mentre salgono.",
        "Live: fix rimpicciolimento barra messaggi quando si tappa il cuore (graphicsLayer).",
        "Live: allineamento verticale corretto tra cuore e pill di input."
    )
}
