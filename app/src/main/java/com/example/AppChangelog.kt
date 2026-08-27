package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.11.6"
    val LINES = listOf(
        "Chat: orario dentro il messaggio e colore diverso tra inviati e ricevuti.",
        "Chat: rimossi i suggerimenti automatici; i messaggi si caricano a blocchi (scorri su per i più vecchi).",
        "Notifiche: toccandole ti portano al punto giusto (chat in fondo, dettaglio live, richieste)."
    )
}
