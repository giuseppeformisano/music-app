package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.10"
    val LINES = listOf(
        "Fix: le notifiche live ora non arrivano ad ogni cambio traccia (Amazon Music / Spotify Free).",
        "Fix: il toggle \"Disattiva notifiche live\" ora funziona correttamente.",
        "Le dialog ora si chiudono con lo swipe sia verso il basso sia verso l'alto.",
        "Nuovo pulsante \"Imposta copertina\" con conferma \"✓ Fatto\" nel dettaglio brano.",
        "Nel dettaglio live tocca la copertina dell'album per aprire il dettaglio brano completo."
    )
}
