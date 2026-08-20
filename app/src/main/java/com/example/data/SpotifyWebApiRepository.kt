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

    suspend fun getCurrentlyPlaying(context: Context): Track? = withContext(Dispatchers.IO) {
        val token = SpotifyAuthRepository.getValidAccessToken(context) ?: return@withContext null
        try {
            val response = client.newCall(
                Request.Builder()
                    .url("https://api.spotify.com/v1/me/player/currently-playing")
                    .header("Authorization", "Bearer $token")
                    .build()
            ).execute()

            if (response.code == 204) {
                Log.d(TAG, "getCurrentlyPlaying: 204 niente in riproduzione")
                return@withContext null
            }
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "getCurrentlyPlaying: HTTP ${response.code} — $errorBody")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            parseTrack(body)
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentlyPlaying error: ${e.message}")
            null
        }
    }

    private fun parseTrack(json: String): Track? {
        return try {
            val obj = JSONObject(json)
            if (!obj.optBoolean("is_playing", false)) return null
            val item = obj.optJSONObject("item") ?: return null
            val title = item.optString("name").takeIf { it.isNotEmpty() } ?: return null
            val artist = item.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: return null
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
                accentColorHex = 0xFF1DB954L,
                genre = "",
                releaseYear = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseTrack error: ${e.message}")
            null
        }
    }

    private fun formatDuration(ms: Long): String {
        val secs = ms / 1000
        return "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"
    }
}
