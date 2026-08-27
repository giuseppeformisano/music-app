package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.2"
    val LINES = listOf(
        "Foto profilo in chat: risolto il problema di visualizzazione dell'avatar della persona in cima alla chat con caricamento istantaneo e fallback."
    )
}
