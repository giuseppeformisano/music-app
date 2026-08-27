package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.0"
    val LINES = listOf(
        "Chat allineata: l'intestazione superiore della chat è perfettamente allineata con l'altezza dei tasti del profilo utente.",
        "Tastiera in sovrapposizione: all'apertura della tastiera non viene più effettuato slide-up né compressione della schermata."
    )
}
