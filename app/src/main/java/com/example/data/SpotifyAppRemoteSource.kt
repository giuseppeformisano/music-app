package com.example.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

/**
 * Sorgente di rilevamento "now playing" basata sullo **Spotify App Remote SDK**.
 *
 * A differenza del notification listener NON richiede il permesso di accesso alle
 * notifiche (che molti OEM negano/revocano), e a differenza della Web API funziona
 * sia per account Free che Premium: si collega direttamente all'app Spotify installata
 * sul telefono e riceve il PlayerState in tempo reale.
 *
 * Limiti: rileva solo quando il player è Spotify e la riproduzione è sul device locale
 * (non Spotify Connect su speaker/cast). Per quei casi restano listener + Web API come
 * fallback, gestiti dal ViewModel.
 */
object SpotifyAppRemoteSource {

    private const val TAG = "SpotifyAppRemote"
    private const val SPOTIFY_PACKAGE = "com.spotify.music"
    // Heartbeat progressi: lo PlayerState emette solo sui cambi di stato, quindi stimiamo
    // la posizione tra un evento e l'altro per far scorrere le statistiche d'ascolto.
    private const val PROGRESS_TICK_MS = 20_000L

    private var appRemote: SpotifyAppRemote? = null
    val isConnected: Boolean get() = appRemote?.isConnected == true

    private val mainHandler = Handler(Looper.getMainLooper())

    // Ultimo stato noto, per stimare la posizione nei tick di heartbeat.
    private var lastTrackUri: String? = null
    private var lastPositionMs: Long = 0L
    private var lastDurationMs: Long = 0L
    private var lastIsPaused: Boolean = true
    private var lastStateAt: Long = 0L

    private var onProgressCb: ((positionMs: Long, durationMs: Long) -> Unit)? = null

    private val progressTicker = object : Runnable {
        override fun run() {
            val cb = onProgressCb
            if (cb != null && isConnected && !lastIsPaused && lastDurationMs > 0) {
                val elapsed = System.currentTimeMillis() - lastStateAt
                val estimated = (lastPositionMs + elapsed).coerceAtMost(lastDurationMs)
                cb(estimated, lastDurationMs)
            }
            if (isConnected) mainHandler.postDelayed(this, PROGRESS_TICK_MS)
        }
    }

    /** True se l'app Spotify è installata (richiede la <queries> nel manifest su Android 11+). */
    fun isSpotifyInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0)
        true
    } catch (e: Exception) {
        false
    }

    /**
     * Si collega all'app Spotify e si iscrive al PlayerState.
     *
     * @param onTrack cambio brano: (titolo, artista, album, durataMs, posizioneMs, imageUri)
     * @param onProgress heartbeat posizione per lo stesso brano
     * @param onStopped riproduzione terminata/nessun brano
     * @param onConnected connessione riuscita
     * @param onFailure connessione fallita (il ViewModel attiva i fallback)
     */
    fun connect(
        context: Context,
        onTrack: (title: String, artist: String, album: String, durationMs: Long, positionMs: Long, imageUri: String) -> Unit,
        onProgress: (positionMs: Long, durationMs: Long) -> Unit,
        onStopped: () -> Unit,
        onConnected: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (isConnected) { onConnected(); return }
        onProgressCb = onProgress

        val params = ConnectionParams.Builder(SpotifyAuthRepository.CLIENT_ID)
            .setRedirectUri(SpotifyAuthRepository.REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context.applicationContext, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                appRemote = remote
                Log.d(TAG, "App Remote connesso")
                remote.playerApi.subscribeToPlayerState().setEventCallback { state ->
                    val track = state.track
                    if (track == null) {
                        lastTrackUri = null
                        onStopped()
                        return@setEventCallback
                    }
                    val now = System.currentTimeMillis()
                    lastPositionMs = state.playbackPosition
                    lastDurationMs = track.duration
                    lastIsPaused = state.isPaused
                    lastStateAt = now

                    val imageUri = track.imageUri?.raw ?: ""
                    if (track.uri != lastTrackUri) {
                        // Nuovo brano
                        lastTrackUri = track.uri
                        onTrack(
                            track.name ?: "",
                            track.artist?.name ?: "",
                            track.album?.name ?: "",
                            track.duration,
                            state.playbackPosition,
                            imageUri
                        )
                    } else {
                        // Stesso brano: seek/play/pause → aggiorna la posizione
                        onProgress(state.playbackPosition, track.duration)
                    }
                }
                mainHandler.removeCallbacks(progressTicker)
                mainHandler.postDelayed(progressTicker, PROGRESS_TICK_MS)
                onConnected()
            }

            override fun onFailure(error: Throwable) {
                appRemote = null
                Log.w(TAG, "App Remote connessione fallita: ${error.message}")
                onFailure(error)
            }
        })
    }

    fun disconnect() {
        mainHandler.removeCallbacks(progressTicker)
        onProgressCb = null
        lastTrackUri = null
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
    }
}
