package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.14.8"
    val LINES = listOf(
        "Dettaglio brano/live: nuova pill 'ascolta su' segmentata con sorgente evidenziata.",
        "Dettaglio feed: rimossa la barra reaction (diamante/onda/stella).",
        "Live: pill 'ascolta su' aggiunta anche nel dettaglio live."
    )
}
