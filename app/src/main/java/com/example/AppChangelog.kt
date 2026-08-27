package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.12.4"
    val LINES = listOf(
        "Chat e Profilo allineati: l'intestazione superiore della chat ha ora lo stesso padding e altezza dei pulsanti del profilo utente.",
        "Tastiera in sovrapposizione: all'apertura della tastiera non viene più effettuato slide-up né compressione, sovrapponendosi in modo pulito."
    )
}
