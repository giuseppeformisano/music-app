package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.40"
    val LINES = listOf(
        "Feed: aura dorata e badge 'sync' quando ascolti lo stesso brano di un amico in tempo reale.",
        "Brano: contatore di quanti utenti nell'app stanno ascoltando lo stesso brano.",
        "Brano: contatore dei tuoi ascolti personali (ogni 30s di ascolto = 1 ascolto).",
        "Brano: reazioni emoji con contatore sessione."
    )
}
