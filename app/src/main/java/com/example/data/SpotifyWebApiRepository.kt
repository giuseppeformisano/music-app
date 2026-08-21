package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object SpotifyWebApiRepository {

    private const val TAG = "SpotifyWebApi"
    private val client = OkHttpClient()

    /**
     * Esito distinto per non azzerare la live su errori transitori:
     * - Playing: brano in riproduzione
     * - NotPlaying: Spotify conferma che non suona nulla (204 / is_playing=false)
     * - Unknown: errore/rete/token — stato ignoto, NON modificare la live
     */
    sealed class PlaybackResult {
        data class Playing(val track: Track, val progressMs: Long) : PlaybackResult()
        object NotPlaying : PlaybackResult()
        // error != null quando c'è un problema di auth/permessi da mostrare all'utente
        // (es. 403 = account non abilitato nell'app). null = errore transitorio (rete).
        data class Unknown(val error: String? = null) : PlaybackResult()
    }

    private data class HttpResult(val code: Int, val body: String?)

    private fun get(url: String, token: String): HttpResult {
        val resp = client.newCall(
            Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        ).execute()
        return HttpResult(resp.code, resp.body?.string())
    }

    suspend fun getCurrentlyPlaying(context: Context): PlaybackResult = withContext(Dispatchers.IO) {
        try {
            val token = SpotifyAuthRepository.getValidAccessToken(context)
                ?: return@withContext PlaybackResult.Unknown("Sessione Spotify assente: ricollega l'account")

            // Passo 1: currently-playing
            val r1 = get("https://api.spotify.com/v1/me/player/currently-playing", token)
            authError(r1.code)?.let { return@withContext PlaybackResult.Unknown(it) }
            if (r1.code != 204 && r1.code in 200..299 && r1.body != null) {
                parsePlayback(r1.body)?.let { return@withContext it }
            }

            // Passo 2 - Fallback: /v1/me/player, più affidabile, riporta anche il device attivo
            val r2 = get("https://api.spotify.com/v1/me/player", token)
            authError(r2.code)?.let { return@withContext PlaybackResult.Unknown(it) }
            if (r2.code == 204) return@withContext PlaybackResult.NotPlaying
            if (r2.code in 200..299 && r2.body != null) {
                parsePlayback(r2.body)?.let { return@withContext it }
            }

            // 204 su currently-playing e nessun device attivo → davvero nulla in play
            if (r1.code == 204) PlaybackResult.NotPlaying else PlaybackResult.Unknown()
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentlyPlaying error: ${e.message}")
            PlaybackResult.Unknown() // transitorio: non toccare la live
        }
    }

    // Messaggio utente per errori di autorizzazione; null se non è un errore auth
    private fun authError(code: Int): String? = when (code) {
        401 -> "Sessione Spotify scaduta: ricollega l'account"
        403 -> "Spotify 403: aggiungi il tuo account tra gli utenti dell'app nel Developer Dashboard (o esci dalla Development Mode)"
        else -> null
    }

    private fun parsePlayback(body: String): PlaybackResult? {
        val track = parseTrack(body) ?: return null
        val progressMs = JSONObject(body).optLong("progress_ms", 0L)
        return PlaybackResult.Playing(track, progressMs)
    }

    private fun parseTrack(json: String): Track? {
        return try {
            val obj = JSONObject(json)
            if (!obj.optBoolean("is_playing", false)) return null
            val item = obj.optJSONObject("item") ?: return null
            val title = item.optString("name").takeIf { it.isNotEmpty() } ?: return null
            val rawArtist = item.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: return null
            val artist = sanitizeContext(rawArtist)
            val album = item.optJSONObject("album")
            val albumName = album?.optString("name") ?: ""
            val coverUrl = album?.optJSONArray("images")?.optJSONObject(0)?.optString("url") ?: ""
            val durationMs = item.optLong("duration_ms", 0L)
            val uri = item.optString("uri")
            Track(
                id = uri,
                title = title,
                artist = artist,
                album = albumName,
                coverUrl = coverUrl,
                durationText = formatDuration(durationMs),
                durationMs = durationMs,
                accentColorHex = 0xFF1DB954L,
                genre = "",
                releaseYear = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseTrack error: ${e.message}")
            null
        }
    }

    // Match esatto (mai substring) per non toccare artisti reali con quelle parole nel nome
    private fun sanitizeContext(value: String): String {
        val contexts = setOf(
            "consigliato per te", "consigliati per te",
            "fatto per te", "made for you",
            "radio", "mix del giorno", "daily mix"
        )
        return if (value.trim().lowercase() in contexts) "" else value.trim()
    }

    private fun formatDuration(ms: Long): String {
        val secs = ms / 1000
        return "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"
    }
}
