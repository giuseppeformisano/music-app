package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.14.7"
    val LINES = listOf(
        "Feed/Live: carosello avatar di chi ascolta lo stesso brano in app (global listeners).",
        "Dettaglio brano: chip 'Ascolta su...' apre bottom sheet con le piattaforme.",
        "Live: sezione global listeners sopra il contenuto centrale.",
        "Live: broadcaster vede i cuori in real-time dal feed."
    )
}
