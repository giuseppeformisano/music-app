package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.25.2"
    val LINES = listOf(
        "Spotify: ripristinato il rilevamento corretto — online e live solo mentre ascolti davvero, la traccia si aggiorna e si chiude quando smetti.",
        "Rimossa la notifica Spotify indesiderata nella barra di stato.",
    )
}
