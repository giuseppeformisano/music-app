package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.7"
    val LINES = listOf(
        "Freccia indietro in alto a sinistra anche nel dettaglio feed e nel dettaglio live, nella stessa posizione degli altri pannelli.",
        "Dettaglio feed: nome, copertina, titolo, artista, genere/anno e ascolto ora equidistanziati verticalmente.",
    )
}
