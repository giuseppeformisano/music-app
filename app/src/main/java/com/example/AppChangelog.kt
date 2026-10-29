package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.0"
    val LINES = listOf(
        "Nuova navigazione: barra in alto con ricerca · notifiche · logo · messaggi · profilo. Niente più barra in basso.",
        "Profilo: indietro e impostazioni in alto; il logout è dentro Impostazioni.",
        "I pannelli (notifiche, chat, dettaglio…) si chiudono con swipe o indietro, senza barra.",
    )
}
