package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.39"
    val LINES = listOf(
        "Brano: bottoni per aprire direttamente su Spotify, Apple Music, Amazon Music e YouTube Music.",
        "Feed: limite di 3 condivisioni al giorno con badge sul FAB e conteggio slot rimanenti.",
        "Limite: bottom sheet informativo con countdown al reset degli slot."
    )
}
