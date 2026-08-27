package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.12.5"
    val LINES = listOf(
        "Notifica live di nuovo corretta: parte solo all'inizio effettivo di un brano, non quando riapri l'app.",
        "Pulse più personale: al posto del cuoricino ora compare la foto del mittente (notifica, chat e schermata dedicata)."
    )
}
