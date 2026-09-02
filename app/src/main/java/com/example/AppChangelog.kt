package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.34"
    val LINES = listOf(
        "Feed: brani raggruppati per settimana e utente.",
        "Feed: carosello con transizione blur+slide tra i brani della stessa settimana.",
        "Feed: timestamp di condivisione salvato per ogni brano."
    )
}
