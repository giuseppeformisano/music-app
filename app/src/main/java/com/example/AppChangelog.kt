package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.6"
    val LINES = listOf(
        "Badge su notifiche e messaggi non più tagliato.",
        "Le notifiche ora mostrano l'orario (stile social) e restano come traccia: dopo aver accettato una richiesta compare il tasto Segui per il follow-back.",
    )
}
