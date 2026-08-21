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
        if (sbn.packageName in MUSIC_PACKAGES) checkMediaSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Rivaluta: se un'altra app musicale sta ancora suonando la live resta;
        // altrimenti checkMediaSessions farà stopPlayback.
        if (sbn.packageName in MUSIC_PACKAGES) checkMediaSessions()
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
            // Prima app musicale supportata effettivamente IN RIPRODUZIONE (Spotify o Amazon)
            val controller = controllers.firstOrNull {
                it.packageName in MUSIC_PACKAGES && isPlaying(it)
            }
            if (controller == null) { stopPlayback(); return }

            val meta = controller.metadata ?: return

            // Filtra le pubblicità di Spotify Free (solo per Spotify): durante un ad NON
            // aggiorniamo la live, resta il brano precedente. Match esatto, mai substring.
            if (controller.packageName == SPOTIFY_PACKAGE && isAdvertisement(meta)) return

            val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: return
            val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?: ""
            val durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
            val positionMs = controller.playbackState?.position?.coerceAtLeast(0L) ?: 0L
            // Artwork esatto dalla MediaSession (se Spotify espone un URL http/https)
            val artUrl = listOf(
                MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                MediaMetadata.METADATA_KEY_ART_URI,
                MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI
            ).firstNotNullOfOrNull { key ->
                meta.getString(key)?.takeIf { it.startsWith("http", ignoreCase = true) }
            } ?: ""
            if (title == lastTrack) {
                // Stesso brano: aggiorna solo la posizione reale (avanzamento/seek)
                onProgressChanged?.invoke(positionMs, durationMs)
                return
            }
            lastTrack = title
            pendingTrack = Pending(title, artist, durationMs, positionMs, artUrl)
            onTrackChanged?.invoke(title, artist, durationMs, positionMs, artUrl)
        } catch (_: Exception) {}
    }

    private fun isPlaying(controller: android.media.session.MediaController): Boolean {
        val state = controller.playbackState?.state
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
    }

    /**
     * Rileva le pubblicità di Spotify Free. Segnale primario: METADATA_KEY_ADVERTISEMENT
     * (Long: 1 = ad). Fallback: un ad Spotify ha durata assente/nulla, artista vuoto e
     * titolo esattamente "Advertisement"/"Spotify" (match esatto, mai substring, così i
     * brani che contengono la parola "pubblicità" non vengono scartati per errore).
     */
    private fun isAdvertisement(meta: MediaMetadata): Boolean {
        // METADATA_KEY_ADVERTISEMENT esiste solo in MediaMetadataCompat, non nel framework:
        // usiamo la chiave grezza (stesso valore). getLong ritorna 0 se assente.
        if (meta.getLong(KEY_ADVERTISEMENT) == 1L) return true
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
        val duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val adTitle = title.equals("Advertisement", ignoreCase = true) ||
                      title.equals("Spotify", ignoreCase = true)
        return adTitle && artist.isEmpty() && duration <= 0L
    }

    data class Pending(val title: String, val artist: String, val durationMs: Long, val positionMs: Long, val artUrl: String)

    companion object {
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
        private const val AMAZON_MUSIC_PACKAGE = "com.amazon.mp3"
        // App musicali supportate dal rilevamento notifiche (MediaSession)
        private val MUSIC_PACKAGES = setOf(SPOTIFY_PACKAGE, AMAZON_MUSIC_PACKAGE)
        private const val KEY_ADVERTISEMENT = "android.media.metadata.ADVERTISEMENT"

        @Volatile var pendingTrack: Pending? = null

        var onTrackChanged: ((trackName: String, artist: String, durationMs: Long, positionMs: Long, artUrl: String) -> Unit)? = null
            set(value) {
                field = value
                pendingTrack?.let { value?.invoke(it.title, it.artist, it.durationMs, it.positionMs, it.artUrl) }
            }

        // Aggiornamento posizione mentre lo stesso brano continua
        var onProgressChanged: ((positionMs: Long, durationMs: Long) -> Unit)? = null

        var onPlaybackStopped: (() -> Unit)? = null

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(context.packageName)
        }
    }
}
