package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.16"
    val LINES = listOf(
        "Fix: chiudendo Spotify Premium la live si azzera subito (isLiveNow=false esplicito)."
    )
}
