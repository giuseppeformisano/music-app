package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Stub del Spotify App Remote SDK.
 * Per l'integrazione reale:
 * 1. Scaricare spotify-app-remote-release-X.X.X.aar da https://github.com/spotify/android-sdk/releases
 * 2. Aggiungere il file in app/libs/
 * 3. Aggiungere in build.gradle.kts: implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
 * 4. Sostituire questo file con l'implementazione reale usando SpotifyAppRemote API.
 */
object SpotifyRepository {

    private const val TAG = "SpotifyRepository"

    val isConnected: Boolean = false

    fun connect(
        context: Context,
        onConnected: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        Log.w(TAG, "Spotify App Remote SDK non ancora integrato. Aggiungere l'AAR in app/libs/.")
        onFailure(UnsupportedOperationException("Spotify SDK non disponibile — aggiungere l'AAR in app/libs/"))
    }

    fun disconnect() {
        // no-op finché l'SDK non è integrato
    }

    fun observePlayerState(): Flow<Nothing> = emptyFlow()

    fun formatSpotifyTrack(playerState: Any?, artworkUrl: String, accentColor: Long): Track? = null
}
