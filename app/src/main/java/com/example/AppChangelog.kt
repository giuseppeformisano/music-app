package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.2"
    val LINES = listOf(
        "Chat e notifiche: tasto indietro nell'header.",
        "Top bar: più spazio tra messaggi e profilo.",
        "Corretto un errore di compilazione della build precedente.",
    )
}
