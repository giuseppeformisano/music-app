package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.9"
    val LINES = listOf(
        "Foto profilo e copertina ora più nitide: caricate su Cloudinary ad alta risoluzione invece di essere compresse nel documento.",
    )
}
