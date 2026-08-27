package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.11.7"
    val LINES = listOf(
        "Presenza: risolto il caso in cui una persona restava 'live' o 'online' pur non essendolo più.",
        "Messaggi: ora puoi cercare una persona a cui scrivere direttamente dalla barra in alto della lista messaggi."
    )
}
