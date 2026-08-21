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
        object Unknown : PlaybackResult()
    }

    suspend fun getCurrentlyPlaying(context: Context): PlaybackResult = withContext(Dispatchers.IO) {
        try {
            val token = SpotifyAuthRepository.getValidAccessToken(context)
                ?: return@withContext PlaybackResult.Unknown
            val response = client.newCall(
                Request.Builder()
                    .url("https://api.spotify.com/v1/me/player/currently-playing")
                    .header("Authorization", "Bearer $token")
                    .build()
            ).execute()

            if (response.code == 204) {
                Log.d(TAG, "getCurrentlyPlaying: 204 niente in riproduzione")
                return@withContext PlaybackResult.NotPlaying
            }
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "getCurrentlyPlaying: HTTP ${response.code} — $errorBody")
                // Errore server/auth: stato ignoto, non azzerare la live
                return@withContext PlaybackResult.Unknown
            }
            val body = response.body?.string() ?: return@withContext PlaybackResult.Unknown
            val track = parseTrack(body)
            // parseTrack ritorna null se is_playing=false → NotPlaying; altrimenti Playing
            if (track != null) {
                val progressMs = JSONObject(body).optLong("progress_ms", 0L)
                PlaybackResult.Playing(track, progressMs)
            } else PlaybackResult.NotPlaying
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentlyPlaying error: ${e.message}")
            // Rete/timeout/doze in background: stato ignoto, non azzerare la live
            PlaybackResult.Unknown
        }
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
