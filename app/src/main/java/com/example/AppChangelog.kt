package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.25.1"
    val LINES = listOf(
        "I pannelli ora escono con la transizione orizzontale inversa (come tornando indietro dal profilo).",
        "Chat: niente più striscia colorata sotto e apertura più veloce.",
    )
}
