package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.18.0"
    val LINES = listOf(
        "Brano del giorno: ora si condivide un solo brano al giorno, stile BeReal.",
        "Feed: corretto il raggruppamento settimanale — i brani vecchi non finiscono più in \"Questa settimana\".",
        "Feed: i brani senza data finiscono nel gruppo \"In precedenza\".",
    )
}
