package com.example

/**
 * Changelog brevissimo della versione corrente, mostrato una sola volta dopo l'aggiornamento.
 * Aggiornare TITLE e LINES ad ogni push con le novità/fix di quella versione.
 */
object AppChangelog {
    const val TITLE = "Novità · 0.15.13"
    val LINES = listOf(
        "Chat: striscia colorata durante swipe eliminata definitivamente.",
        "Chat: badge messaggi non letti sull'icona messaggi in basso.",
        "Chat: spunte stile WhatsApp — grigia inviato, bianca letto.",
        "Chat: swipe al bordo (su/giù) chiude la dialog; scroll veloce e fling non la chiudono.",
        "Impostazioni accessibili direttamente da Live e Feed.",
        "Spotify Premium: rilevamento brano istantaneo via listener, zero polling.",
    )
}
