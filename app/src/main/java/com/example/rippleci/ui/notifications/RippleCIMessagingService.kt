package com.example.rippleci.ui.notifications

import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.rippleci.MainActivity
import com.example.rippleci.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RippleCIMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "New Notification"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
        val conversationId = remoteMessage.data["conversationId"]
        val type = remoteMessage.data["type"]
        val requestId = remoteMessage.data["requestId"]
        showNotification(title, body, conversationId, type, requestId)
    }

    private fun showNotification(
        title: String,
        body: String,
        conversationId: String? = null,
        type: String? = null,
        requestId: String? = null
    ) {
        val channelId = if (type == "friend_request") {
            "friend_requests_channel"
        } else {
            "messages_channel"
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messagesChannel = NotificationChannel(
                "messages_channel",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(messagesChannel)

            val friendChannel = NotificationChannel(
                "friend_requests_channel",
                "Friend Requests",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(friendChannel)
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        when (type) {
            "friend_request" -> {
                intent.putExtra("navigate_to", "friends")
                if (requestId != null) {
                    intent.putExtra("requestId", requestId)
                }
            }
            else -> {
                intent.putExtra("navigate_to", "messages")
                if (conversationId != null) {
                    intent.putExtra("conversationId", conversationId)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
    }
}