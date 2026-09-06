package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.14.9"
    val LINES = listOf(
        "Nuovo play 'ascolta su': tap e i loghi si aprono a mezzaluna 180° sotto.",
        "Loghi brand (Spotify, Amazon Music, YouTube) con alone luminoso, fusi col fondo.",
        "Presente sia nel dettaglio feed che nel dettaglio live."
    )
}
