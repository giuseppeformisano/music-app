package com.example

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
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
            val spotify = controllers.firstOrNull { it.packageName == SPOTIFY_PACKAGE } ?: return
            val meta = spotify.metadata ?: return
            val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: return
            val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?: ""
            if (title == lastTrack) return
            lastTrack = title
            pendingTrack = title to artist
            onTrackChanged?.invoke(title, artist)
        } catch (_: Exception) {}
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
