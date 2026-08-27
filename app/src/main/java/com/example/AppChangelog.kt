package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.11.8"
    val LINES = listOf(
        "Chat: sfondo personalizzabile, bolle con bordino colorato distinto per persona e barra di testo che non copre più i tasti di sistema.",
        "Messaggi: il pannello si chiude con lo swipe verso il basso, come gli altri.",
        "Privacy: lo stato di una persona è visibile solo se la segui."
    )
}
