package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.14.2"
    val LINES = listOf(
        "Live: cuore 2D appena sopra il pulsante, vola e svanisce in ~280ms.",
        "Live: tap continuo senza blocchi, scala indipendente per ogni pressione.",
        "Live: contatore liveHearts aggiorna updatedAt → listener Firestore notifica tutti."
    )
}
