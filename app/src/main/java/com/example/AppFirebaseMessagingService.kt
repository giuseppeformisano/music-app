package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.viewmodel.MusicViewModel
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        onTokenRefreshed?.invoke(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val isLive = data["type"] == "live_start" || data["open_live"] == "true"
        if (isLive) {
            val liveNotifsEnabled = try {
                getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                    .getBoolean("live_notifications_enabled", true)
            } catch (_: Exception) { true }
            if (!liveNotifsEnabled) return
        }

        val hostUserId = data["hostUserId"] ?: ""

        val defaultTitle = if (isLive) "Diretta Live 🎵" else "Nuova richiesta di follow"
        val title = data["title"] ?: message.notification?.title ?: defaultTitle
        val body = data["body"] ?: message.notification?.body ?: ""

        val notifKey = data["requestId"] ?: data["fromUserId"] ?: hostUserId.ifBlank { "${title}_$body" }
        val avatarUrl = data["avatarUrl"] ?: data["fromUserAvatarUrl"] ?: ""

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val avatarBitmap = if (avatarUrl.isNotBlank()) com.example.data.ImageUtils.loadAvatarBitmap(this@AppFirebaseMessagingService, avatarUrl) else null

            val intent = android.content.Intent(this@AppFirebaseMessagingService, MainActivity::class.java).apply {
                if (isLive) {
                    putExtra(MainActivity.EXTRA_OPEN_LIVE, true)
                    putExtra("open_live", "true")
                    putExtra("type", "live_start")
                    putExtra("hostUserId", hostUserId)
                } else {
                    putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
                    putExtra("open_notifications", "true")
                    putExtra("type", "follow_request")
                }
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this@AppFirebaseMessagingService,
                notifKey.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notifBuilder = NotificationCompat.Builder(this@AppFirebaseMessagingService, MusicViewModel.FRIEND_REQUEST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (avatarBitmap != null) {
                notifBuilder.setLargeIcon(avatarBitmap)
            }

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(notifKey.hashCode(), notifBuilder.build())
        }
    }

    companion object {
        var onTokenRefreshed: ((String) -> Unit)? = null
    }
}
