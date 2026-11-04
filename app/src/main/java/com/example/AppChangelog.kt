package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.26.41"
    val LINES = listOf(
        "Ascoltatori nel mondo: il conteggio si aggiorna da solo quando il brano live cambia.",
        "Al cambio brano il badge esce verso l'alto e il nuovo rientra dal basso con dissolvenza sfocata.",
    )
}
