package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
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
        val isChat = data["type"] == "new_message" || data["open_chat"] == "true"
        val isPulse = data["type"] == "new_pulse" || data["open_pulse"] == "true"
        val pulse = data["pulse"] ?: ""
        if (isLive) {
            val liveNotifsEnabled = try {
                getSharedPreferences("user_settings", android.content.Context.MODE_PRIVATE)
                    .getBoolean("live_notifications_enabled", true)
            } catch (_: Exception) { true }
            if (!liveNotifsEnabled) return
        }

        val hostUserId = data["hostUserId"] ?: ""
        val senderId = data["senderId"] ?: ""

        // App in primo piano: la live è già visibile nella lista → niente notifica di sistema.
        if (isLive && isAppForeground) return
        // Stai già guardando questa chat → niente notifica per i suoi messaggi.
        if (isChat && isAppForeground && openChatUserId == senderId && senderId.isNotBlank()) return

        val defaultTitle = when {
            isLive -> "Diretta Live 🎵"
            isPulse -> "Nuovo Pulse"
            isChat -> "Nuovo messaggio"
            else -> "Nuova richiesta di follow"
        }
        val title = data["title"] ?: message.notification?.title ?: defaultTitle
        val body = data["body"] ?: message.notification?.body ?: ""

        val notifKey = data["requestId"] ?: data["fromUserId"]
            ?: (if (isChat) senderId else hostUserId).ifBlank { "${title}_$body" }
        val avatarUrl = data["avatarUrl"] ?: data["fromUserAvatarUrl"] ?: ""

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val avatarBitmap = if (avatarUrl.isNotBlank()) com.example.data.ImageUtils.loadAvatarBitmap(this@AppFirebaseMessagingService, avatarUrl) else null

            val intent = android.content.Intent(this@AppFirebaseMessagingService, MainActivity::class.java).apply {
                when {
                    isLive -> {
                        putExtra(MainActivity.EXTRA_OPEN_LIVE, true)
                        putExtra("open_live", "true")
                        putExtra("type", "live_start")
                        putExtra("hostUserId", hostUserId)
                    }
                    isPulse -> {
                        putExtra(MainActivity.EXTRA_OPEN_PULSE, true)
                        putExtra("open_pulse", "true")
                        putExtra("type", "new_pulse")
                        putExtra("senderId", senderId)
                        putExtra("senderName", data["senderName"] ?: "")
                        putExtra("avatarUrl", avatarUrl)
                        putExtra("pulse", pulse)
                    }
                    isChat -> {
                        putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
                        putExtra("open_chat", "true")
                        putExtra("type", "new_message")
                        putExtra("senderId", senderId)
                    }
                    else -> {
                        putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
                        putExtra("open_notifications", "true")
                        putExtra("type", "follow_request")
                    }
                }
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this@AppFirebaseMessagingService,
                notifKey.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (isChat) {
                // Stile WhatsApp: i messaggi della STESSA chat si accorpano in UNA notifica
                // (stesso id = senderId), aggiungendo la nuova riga a quelle già presenti.
                val senderName = (data["senderName"]?.takeIf { it.isNotBlank() }) ?: title
                val senderPerson = Person.Builder()
                    .setName(senderName)
                    .apply { if (avatarBitmap != null) setIcon(IconCompat.createWithBitmap(avatarBitmap)) }
                    .build()

                val existing = nm.activeNotifications.firstOrNull { it.id == notifKey.hashCode() }?.notification
                val style = (existing?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) })
                    ?: NotificationCompat.MessagingStyle(Person.Builder().setName("Tu").build())
                style.addMessage(body, System.currentTimeMillis(), senderPerson)

                val chatNotif = NotificationCompat.Builder(this@AppFirebaseMessagingService, MusicViewModel.FRIEND_REQUEST_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setStyle(style)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .build()
                nm.notify(notifKey.hashCode(), chatNotif)
                return@launch
            }

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

            nm.notify(notifKey.hashCode(), notifBuilder.build())
        }
    }

    companion object {
        var onTokenRefreshed: ((String) -> Unit)? = null
        // Stato UI condiviso col servizio FCM per non disturbare con notifiche ridondanti:
        // se l'app è in primo piano non serve notificare le live (le vedi già nella lista);
        // se stai guardando proprio quella chat non serve notificare i suoi messaggi.
        @Volatile var isAppForeground: Boolean = false
        @Volatile var openChatUserId: String? = null
    }
}
