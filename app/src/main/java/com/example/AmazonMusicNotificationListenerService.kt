package com.example

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class AmazonMusicNotificationListenerService : NotificationListenerService() {

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
        if (sbn.packageName == AMAZON_MUSIC_PACKAGE) checkMediaSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != AMAZON_MUSIC_PACKAGE) return
        // Non terminare la riproduzione a priori (Amazon Music rimuove e ricrea notifiche):
        // verifica se la MediaSession è ancora presente ed attiva
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
                ComponentName(this, AmazonMusicNotificationListenerService::class.java)
            )
            val amazonMusic = controllers.firstOrNull { it.packageName == AMAZON_MUSIC_PACKAGE }
            // Sessione assente = app musicale chiusa -> esce dalla live
            if (amazonMusic == null) { stopPlayback(); return }

            val state = amazonMusic.playbackState?.state
            // Riproduzione terminata/chiusa esplicitamente
            if (state == PlaybackState.STATE_STOPPED || state == PlaybackState.STATE_NONE) {
                stopPlayback()
                return
            }

            val meta = amazonMusic.metadata ?: return

            val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: return
            val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?: ""
            val durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
            val positionMs = amazonMusic.playbackState?.position?.coerceAtLeast(0L) ?: 0L
            // Artwork esatto dalla MediaSession (se Amazon Music espone un URL http/https)
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

    data class Pending(val title: String, val artist: String, val durationMs: Long, val positionMs: Long, val artUrl: String)

    companion object {
        private const val AMAZON_MUSIC_PACKAGE = "com.amazon.mp3"

        @Volatile private var instance: AmazonMusicNotificationListenerService? = null

        fun stopListening() {
            try { instance?.requestUnbind() } catch (_: Exception) {}
        }

        fun startListening(context: Context) {
            try {
                requestRebind(ComponentName(context, AmazonMusicNotificationListenerService::class.java))
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
            // Controllo del COMPONENTE specifico (non solo del package)
            val cn = ComponentName(context, AmazonMusicNotificationListenerService::class.java)
            return flat.split(":").any {
                it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
            }
        }
    }
}
