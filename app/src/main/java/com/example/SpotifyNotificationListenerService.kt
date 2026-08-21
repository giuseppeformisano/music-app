package com.example

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SpotifyNotificationListenerService : NotificationListenerService() {

    private var lastTrack = ""

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != SPOTIFY_PACKAGE) return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")?.trim() ?: return
        val artist = extras.getString("android.text")?.trim() ?: ""
        if (title == lastTrack) return
        lastTrack = title
        onTrackChanged?.invoke(title, artist)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != SPOTIFY_PACKAGE) return
        lastTrack = ""
        onPlaybackStopped?.invoke()
    }

    companion object {
        private const val SPOTIFY_PACKAGE = "com.spotify.music"

        var onTrackChanged: ((trackName: String, artist: String) -> Unit)? = null
        var onPlaybackStopped: (() -> Unit)? = null

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(context.packageName)
        }
    }
}
