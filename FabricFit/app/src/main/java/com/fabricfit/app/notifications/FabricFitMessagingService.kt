package com.fabricfit.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fabricfit.app.MainActivity
import com.fabricfit.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging service for handling push notifications.
 */
class FabricFitMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_LIKES = "fabricfit_likes"
        const val CHANNEL_ID_COMMENTS = "fabricfit_comments"
        const val CHANNEL_ID_FOLLOWS = "fabricfit_follows"
        const val CHANNEL_ID_CHALLENGES = "fabricfit_challenges"
        
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        const val KEY_CONTENT_ID = "content_id"
        const val KEY_USER_ID = "user_id"
        
        const val TYPE_LIKE = "like"
        const val TYPE_COMMENT = "comment"
        const val TYPE_FOLLOW = "follow"
        const val TYPE_CHALLENGE = "challenge"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to your backend to associate with user
        // For now, just log it
        android.util.Log.d("FCM", "New token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            handleDataPayload(message.data)
        }

        // Check if message contains notification payload
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "FabricFit",
                body = notification.body ?: "",
                data = message.data,
            )
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val type = data[KEY_NOTIFICATION_TYPE]
        val contentId = data[KEY_CONTENT_ID]
        val userId = data[KEY_USER_ID]

        when (type) {
            TYPE_LIKE -> handleLikeNotification(contentId, userId, data)
            TYPE_COMMENT -> handleCommentNotification(contentId, userId, data)
            TYPE_FOLLOW -> handleFollowNotification(userId, data)
            TYPE_CHALLENGE -> handleChallengeNotification(contentId, data)
        }
    }

    private fun handleLikeNotification(contentId: String?, userId: String?, data: Map<String, String>) {
        val username = data["username"] ?: "Someone"
        val title = getString(R.string.notification_like_title, username)
        val body = getString(R.string.notification_like_body)

        showNotification(
            title = title,
            body = body,
            data = data,
            channelId = CHANNEL_ID_LIKES,
        )
    }

    private fun handleCommentNotification(contentId: String?, userId: String?, data: Map<String, String>) {
        val username = data["username"] ?: "Someone"
        val commentText = data["comment_text"] ?: ""
        val title = getString(R.string.notification_comment_title, username)
        val body = if (commentText.isNotEmpty()) commentText else getString(R.string.notification_comment_body)

        showNotification(
            title = title,
            body = body,
            data = data,
            channelId = CHANNEL_ID_COMMENTS,
        )
    }

    private fun handleFollowNotification(userId: String?, data: Map<String, String>) {
        val username = data["username"] ?: "Someone"
        val title = getString(R.string.notification_follow_title, username)
        val body = getString(R.string.notification_follow_body)

        showNotification(
            title = title,
            body = body,
            data = data,
            channelId = CHANNEL_ID_FOLLOWS,
        )
    }

    private fun handleChallengeNotification(contentId: String?, data: Map<String, String>) {
        val challengeName = data["challenge_name"] ?: "a challenge"
        val title = getString(R.string.notification_challenge_title)
        val body = getString(R.string.notification_challenge_body, challengeName)

        showNotification(
            title = title,
            body = body,
            data = data,
            channelId = CHANNEL_ID_CHALLENGES,
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String = CHANNEL_ID_LIKES,
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channels for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(notificationManager)
        }

        // Create intent for deep linking
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(KEY_NOTIFICATION_TYPE, data[KEY_NOTIFICATION_TYPE])
            putExtra(KEY_CONTENT_ID, data[KEY_CONTENT_ID])
            putExtra(KEY_USER_ID, data[KEY_USER_ID])
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_LIKES,
                    getString(R.string.channel_likes),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = getString(R.string.channel_likes_description)
                },
                NotificationChannel(
                    CHANNEL_ID_COMMENTS,
                    getString(R.string.channel_comments),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = getString(R.string.channel_comments_description)
                },
                NotificationChannel(
                    CHANNEL_ID_FOLLOWS,
                    getString(R.string.channel_follows),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = getString(R.string.channel_follows_description)
                },
                NotificationChannel(
                    CHANNEL_ID_CHALLENGES,
                    getString(R.string.channel_challenges),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = getString(R.string.channel_challenges_description)
                },
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
