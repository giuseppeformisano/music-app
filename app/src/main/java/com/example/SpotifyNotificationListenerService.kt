package com.example

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SpotifyNotificationListenerService : NotificationListenerService() {

    private var lastTrack = ""

    // Chiamato quando Android lega il service — legge subito le notifiche già attive
    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { sbn ->
            if (sbn.packageName == SPOTIFY_PACKAGE) {
                val extras = sbn.notification.extras
                val title = extras.getString("android.title")?.trim() ?: return@forEach
                val artist = extras.getString("android.text")?.trim() ?: ""
                if (title != lastTrack) {
                    lastTrack = title
                    pendingTrack = title to artist   // salva anche se callback è ancora null
                    onTrackChanged?.invoke(title, artist)
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != SPOTIFY_PACKAGE) return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")?.trim() ?: return
        val artist = extras.getString("android.text")?.trim() ?: ""
        if (title == lastTrack) return
        lastTrack = title
        pendingTrack = title to artist
        onTrackChanged?.invoke(title, artist)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != SPOTIFY_PACKAGE) return
        lastTrack = ""
        pendingTrack = null
        onPlaybackStopped?.invoke()
    }

    companion object {
        private const val SPOTIFY_PACKAGE = "com.spotify.music"

        // Ultimo track noto: replayato quando il ViewModel registra il callback in ritardo
        @Volatile var pendingTrack: Pair<String, String>? = null

        var onTrackChanged: ((trackName: String, artist: String) -> Unit)? = null
            set(value) {
                field = value
                // Se il service aveva già ricevuto un track prima che il callback
                // fosse registrato, lo inviamo subito
                pendingTrack?.let { (track, artist) -> value?.invoke(track, artist) }
            }

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
