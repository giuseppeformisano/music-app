package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.21.0"
    val LINES = listOf(
        "Copertina atmosferica ora anche su notifiche, impostazioni, follower e lista chat (se attiva nelle impostazioni).",
        "Chat: corretto il margine superiore dell'header, non più troppo ribassato.",
    )
}
