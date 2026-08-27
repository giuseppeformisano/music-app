package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.12.2"
    val LINES = listOf(
        "Colori chat bicolore: i bordi a sfumatura doppia ora sono nettamente più definiti e contrastanti tra chi invia e chi riceve.",
        "Fusione intelligente: i colori si armonizzano con lo sfondo della chat e sono calcolati in modo indipendente su ogni dispositivo."
    )
}
