package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.1"
    val LINES = listOf(
        "Chat in alto: corretto definitivamente il padding superiore della chat (status bar + 28dp), ora perfettamente distanziata e allineata.",
        "Tastiera in sovrapposizione: la tastiera non sposta né comprime più l'interfaccia."
    )
}
