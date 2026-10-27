package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.20.0"
    val LINES = listOf(
        "Notifiche persistenti stile Instagram: non spariscono più dopo essere state gestite, restano come traccia.",
        "Richieste di follow: dopo aver accettato o rifiutato, se non segui quella persona compare il bottone \"Segui\".",
        "Chat: aprendo una conversazione dalla lista, la chat si apre sopra la lista — chiudendola torni all'elenco.",
    )
}
