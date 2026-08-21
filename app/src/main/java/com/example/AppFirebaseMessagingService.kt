package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.viewmodel.MusicViewModel
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        onTokenRefreshed?.invoke(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Nuova notifica"
        val body = data["body"] ?: message.notification?.body ?: ""

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }

        val notif = NotificationCompat.Builder(this, MusicViewModel.FRIEND_REQUEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    companion object {
        var onTokenRefreshed: ((String) -> Unit)? = null
    }
}
