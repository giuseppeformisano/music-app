package com.example

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SpotifyNotificationListenerService : NotificationListenerService() {

    private var lastTrack = ""

    override fun onListenerConnected() {
        super.onListenerConnected()
        checkMediaSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == SPOTIFY_PACKAGE) checkMediaSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != SPOTIFY_PACKAGE) return
        stopPlayback()
    }

    private fun stopPlayback() {
        lastTrack = ""
        pendingTrack = null
        onPlaybackStopped?.invoke()
    }

    private fun checkMediaSessions() {
        try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = manager.getActiveSessions(
                ComponentName(this, SpotifyNotificationListenerService::class.java)
            )
            val spotify = controllers.firstOrNull { it.packageName == SPOTIFY_PACKAGE }
            if (spotify == null) { stopPlayback(); return }

            // In pausa/stop la live deve sparire anche se la notifica resta visibile
            val state = spotify.playbackState?.state
            val isPlaying = state == PlaybackState.STATE_PLAYING ||
                            state == PlaybackState.STATE_BUFFERING
            if (!isPlaying) { stopPlayback(); return }

            val meta = spotify.metadata ?: return

            // Filtra le pubblicità di Spotify Free: durante un ad NON aggiorniamo la live,
            // resta visibile il brano precedente. Nessun controllo sul nome (un brano può
            // contenere la parola "pubblicità"): usiamo il flag ufficiale del MediaMetadata.
            if (isAdvertisement(meta)) return

            val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: return
            val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?: ""
            if (title == lastTrack) return
            lastTrack = title
            pendingTrack = title to artist
            onTrackChanged?.invoke(title, artist)
        } catch (_: Exception) {}
    }

    /**
     * Rileva le pubblicità di Spotify Free. Segnale primario: METADATA_KEY_ADVERTISEMENT
     * (Long: 1 = ad). Fallback: un ad Spotify ha durata assente/nulla, artista vuoto e
     * titolo esattamente "Advertisement"/"Spotify" (match esatto, mai substring, così i
     * brani che contengono la parola "pubblicità" non vengono scartati per errore).
     */
    private fun isAdvertisement(meta: MediaMetadata): Boolean {
        if (meta.getLong(MediaMetadata.METADATA_KEY_ADVERTISEMENT) == 1L) return true
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
        val duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val adTitle = title.equals("Advertisement", ignoreCase = true) ||
                      title.equals("Spotify", ignoreCase = true)
        return adTitle && artist.isEmpty() && duration <= 0L
    }

    companion object {
        private const val SPOTIFY_PACKAGE = "com.spotify.music"

        @Volatile var pendingTrack: Pair<String, String>? = null

        var onTrackChanged: ((trackName: String, artist: String) -> Unit)? = null
            set(value) {
                field = value
                pendingTrack?.let { (t, a) -> value?.invoke(t, a) }
            }

        var onPlaybackStopped: (() -> Unit)? = null

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(context.packageName)
        }
    }
}
