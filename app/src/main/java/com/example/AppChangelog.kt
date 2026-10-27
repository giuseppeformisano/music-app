package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.20.1"
    val LINES = listOf(
        "Finestre più fluide: eliminate le ricomposizioni e i re-layout durante lo slide delle dialog.",
        "Transizioni swipe su/giù ora scorrono senza scatti.",
    )
}
