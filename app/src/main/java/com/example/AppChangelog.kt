package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.7"
    val LINES = listOf(
        "Le dialog ora si chiudono con lo swipe sia verso il basso sia verso l'alto.",
        "Nuovo pulsante \"Imposta copertina\" nel dettaglio brano: applica l'artwork come sfondo del profilo, con conferma \"✓ Fatto\".",
        "Nel dettaglio live tocca la copertina dell'album per aprire il dettaglio brano completo."
    )
}
