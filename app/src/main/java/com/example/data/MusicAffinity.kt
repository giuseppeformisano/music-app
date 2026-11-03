package com.example.data

/**
 * Calcolo della "sintonia" (affinità musicale) tra due persone.
 *
 * Ogni utente ha una firma dei gusti = quote di tempo d'ascolto per artista e per genere
 * (mappe nome -> frazione 0..1, vedi FirebaseRepository.updateTasteSignature).
 *
 * La sintonia è la SOVRAPPOSIZIONE pesata delle due firme: per ogni voce in comune si somma
 * il minimo delle due quote (overlap). È continua — se ascolti un artista un po' più o un po'
 * meno, il punteggio si muove di poco, senza scalini — e sta sempre tra 0 e 1.
 *
 * Il punteggio finale mescola artisti e generi (gli artisti pesano più dei generi): così due
 * persone con artisti diversi ma stesso genere non vanno a zero.
 */
object MusicAffinity {

    private const val ARTIST_WEIGHT = 0.6
    private const val GENRE_WEIGHT = 0.4

    /** Sovrapposizione di due distribuzioni di quote: somma dei minimi delle voci condivise. */
    private fun overlap(a: Map<String, Double>, b: Map<String, Double>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var sum = 0.0
        for ((key, va) in a) {
            val vb = b[key] ?: continue
            sum += minOf(va, vb)
        }
        return sum.coerceIn(0.0, 1.0)
    }

    /** Punteggio di sintonia 0..100 tra due firme dei gusti. */
    fun score(
        artistsA: Map<String, Double>,
        genresA: Map<String, Double>,
        artistsB: Map<String, Double>,
        genresB: Map<String, Double>
    ): Int {
        val art = overlap(artistsA, artistsB)
        val gen = overlap(genresA, genresB)
        return ((art * ARTIST_WEIGHT + gen * GENRE_WEIGHT) * 100.0).toInt().coerceIn(0, 100)
    }
}
