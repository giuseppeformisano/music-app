package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.17"
    val LINES = listOf(
        "Al primo avvio viene chiesto di abilitare l'accesso alle notifiche.",
        "Banner in-app quando l'accesso alle notifiche viene disabilitato."
    )
}
