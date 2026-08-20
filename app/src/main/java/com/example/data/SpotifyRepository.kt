package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Track
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object SpotifyRepository {

    private const val CLIENT_ID = "d22ae7ca717e4f289e61b6d3f2e40f8b"
    private const val REDIRECT_URI = "com.aistudio.music.livefeed://callback"
    private const val TAG = "SpotifyRepository"

    private var appRemote: SpotifyAppRemote? = null

    val isConnected: Boolean get() = appRemote?.isConnected == true

    fun connect(
        context: Context,
        onConnected: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val params = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                appRemote = remote
                Log.d(TAG, "Spotify App Remote connesso")
                onConnected()
            }

            override fun onFailure(error: Throwable) {
                Log.e(TAG, "Connessione Spotify fallita: ${error.message}")
                onFailure(error)
            }
        })
    }

    fun disconnect() {
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
    }

    fun observePlayerState(): Flow<PlayerState?> = callbackFlow {
        val remote = appRemote
        if (remote == null || !remote.isConnected) {
            close()
            return@callbackFlow
        }
        val subscription = remote.playerApi
            .subscribeToPlayerState()
            .setEventCallback { state -> trySend(state) }
            .setErrorCallback { error -> Log.e(TAG, "Player state error: ${error.message}") }

        awaitClose { subscription.cancel() }
    }

    fun formatSpotifyTrack(playerState: PlayerState?, artworkUrl: String, accentColor: Long): Track? {
        val spotifyTrack = playerState?.track ?: return null
        return Track(
            id = spotifyTrack.uri,
            title = spotifyTrack.name,
            artist = spotifyTrack.artist.name,
            album = spotifyTrack.album.name,
            coverUrl = artworkUrl,
            durationText = formatDuration(spotifyTrack.duration),
            accentColorHex = accentColor,
            genre = "",
            releaseYear = ""
        )
    }

    private fun formatDuration(ms: Long): String {
        val secs = ms / 1000
        return "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"
    }
}
