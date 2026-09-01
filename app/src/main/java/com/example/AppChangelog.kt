package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.30"
    val LINES = listOf(
        "Live: il dettaglio live non cambia più utente da solo quando la lista si aggiorna.",
        "Live: se l'utente chiude la live, il dettaglio si chiude automaticamente.",
        "Live: navigazione next/previous ora stabile anche con lista che cambia."
    )
}
