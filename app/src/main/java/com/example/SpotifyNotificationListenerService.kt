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
        instance = this
        checkMediaSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == SPOTIFY_PACKAGE) checkMediaSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != SPOTIFY_PACKAGE) return
        checkMediaSessions()
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
            // SOLO Spotify: non deve MAI leggere la sessione di altre app (es. Amazon)
            val controller = controllers.firstOrNull { it.packageName == SPOTIFY_PACKAGE }
            // Sessione assente = app musicale chiusa -> esce dalla live
            if (controller == null) { stopPlayback(); return }

            val state = controller.playbackState?.state
            // Riproduzione terminata/chiusa esplicitamente
            if (state == PlaybackState.STATE_STOPPED || state == PlaybackState.STATE_NONE) {
                stopPlayback()
                return
            }

            val meta = controller.metadata ?: return

            // Filtra le pubblicità di Spotify Free: durante un ad NON aggiorniamo la live.
            if (isAdvertisement(meta)) return

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
        private const val KEY_ADVERTISEMENT = "android.media.metadata.ADVERTISEMENT"

        @Volatile private var instance: SpotifyNotificationListenerService? = null

        // Scollega il listener: smette DAVVERO di leggere le notifiche (si ri-aggancia
        // solo con startListening/requestRebind). Non revoca il toggle in Impostazioni
        // (Android non lo consente da codice), ma ne annulla l'effetto.
        fun stopListening() {
            try { instance?.requestUnbind() } catch (_: Exception) {}
        }

        fun startListening(context: Context) {
            try {
                requestRebind(ComponentName(context, SpotifyNotificationListenerService::class.java))
            } catch (_: Exception) {}
        }

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
            // Controllo del COMPONENTE specifico (non solo del package), così Spotify e
            // Amazon si distinguono con precisione anche quando ne abiliti solo uno.
            val cn = ComponentName(context, SpotifyNotificationListenerService::class.java)
            return flat.split(":").any {
                it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
            }
        }
    }
}
