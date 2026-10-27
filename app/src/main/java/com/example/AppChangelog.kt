package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.17.0"
    val LINES = listOf(
        "Profilo: statistiche reali — ore e brani ascoltati, artista top, condivisi, generi, cuori.",
        "Profilo: 6 statistiche su 2 pagine con swipe orizzontale e sfocatura stile Feed/Live.",
        "Ascolto: le ore e i brani ascoltati vengono ora tracciati davvero durante la live.",
        "Notifiche: push quando un amico condivide un brano nel feed — tocca per aprire il suo profilo.",
    )
}
