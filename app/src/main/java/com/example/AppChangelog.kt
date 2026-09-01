package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.13.28"
    val LINES = listOf(
        "Dialog: swipe da qualsiasi punto in LiveDetail, TrackDetail e PulseReceive.",
        "Dialog: NowPlaying e dialogs con scroll interno usano zona handle in alto.",
        "UpdateBanner: non chiudibile con swipe durante aggiornamento."
    )
}
