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
import com.fabricfit.app.analytics.WardrobeAnalytics
import com.fabricfit.app.domain.model.Fabric
import java.util.Calendar

/**
 * Smart notification system for wardrobe insights and reminders.
 */
object SmartNotifications {
    
    private const val CHANNEL_ID = "fabricfit_smart_notifications"
    private const val CHANNEL_NAME = "Smart Recommendations"
    
    /**
     * Initialize notification channels (call in Application onCreate).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Smart recommendations and reminders for your wardrobe"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Check and send relevant notifications based on wardrobe state.
     */
    fun checkAndSendNotifications(context: Context, fabrics: List<Fabric>) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        
        // 1. Check for fabrics not worn in 30 days
        checkForgottenFabrics(context, fabrics, notificationManager)
        
        // 2. Send seasonal recommendations
        sendSeasonalRecommendations(context, fabrics, notificationManager)
        
        // 3. Check for wardrobe gaps
        checkWardrobeGaps(context, fabrics, notificationManager)
        
        // 4. Weekend outfit suggestions
        sendWeekendSuggestions(context, fabrics, notificationManager)
    }
    
    /**
     * Send notification about fabrics not worn recently.
     */
    private fun checkForgottenFabrics(
        context: Context,
        fabrics: List<Fabric>,
        notificationManager: NotificationManager
    ) {
        val forgottenFabrics = WardrobeAnalytics.getFabricsNotWornRecently(fabrics, days = 30)
        
        if (forgottenFabrics.isNotEmpty() && forgottenFabrics.size >= 3) {
            val count = forgottenFabrics.size
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Rediscover Your Wardrobe")
                .setContentText("You have $count fabrics you haven't scanned in 30 days. Time to rediscover them!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent(context))
                .build()
            
            notificationManager.notify(1001, notification)
        }
    }
    
    /**
     * Send seasonal recommendations.
     */
    private fun sendSeasonalRecommendations(
        context: Context,
        fabrics: List<Fabric>,
        notificationManager: NotificationManager
    ) {
        val recommendations = WardrobeAnalytics.getSeasonalRecommendations(fabrics)
        
        if (recommendations.isNotEmpty()) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Seasonal Style Tips")
                .setContentText(recommendations.first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(recommendations.joinToString("\n")))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent(context))
                .build()
            
            notificationManager.notify(1002, notification)
        }
    }
    
    /**
     * Check for wardrobe gaps and suggest improvements.
     */
    private fun checkWardrobeGaps(
        context: Context,
        fabrics: List<Fabric>,
        notificationManager: NotificationManager
    ) {
        val gaps = WardrobeAnalytics.identifyWardrobeGaps(fabrics)
        
        if (gaps.isNotEmpty()) {
            val firstGap = gaps.first()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Complete Your Wardrobe")
                .setContentText("Wardrobe gap: $firstGap")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "Your wardrobe could use some additions:\n${gaps.joinToString("\n")}"
                ))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent(context))
                .build()
            
            notificationManager.notify(1003, notification)
        }
    }
    
    /**
     * Send weekend outfit suggestions.
     */
    private fun sendWeekendSuggestions(
        context: Context,
        fabrics: List<Fabric>,
        notificationManager: NotificationManager
    ) {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        // Send on Friday or Saturday
        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            val favoriteFabrics = WardrobeAnalytics.getMostUsedFabrics(fabrics, limit = 3)
            
            if (favoriteFabrics.isNotEmpty()) {
                val fabricNames = favoriteFabrics.take(2).mapNotNull { it.name }.joinToString(", ")
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Weekend Style Inspiration")
                    .setContentText("Why not wear one of your favorites this weekend? Try $fabricNames!")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .setContentIntent(createPendingIntent(context))
                    .build()
                
                notificationManager.notify(1004, notification)
            }
        }
    }
    
    /**
     * Send notification about color harmony.
     */
    fun sendColorHarmonyNotification(
        context: Context,
        fabrics: List<Fabric>,
        notificationManager: NotificationManager
    ) {
        val suggestions = WardrobeAnalytics.getColorHarmonySuggestions(fabrics)
        
        if (suggestions.isNotEmpty()) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Color Your Wardrobe")
                .setContentText(suggestions.first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(suggestions.joinToString("\n")))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent(context))
                .build()
            
            notificationManager.notify(1005, notification)
        }
    }
    
    /**
     * Send event reminder notification.
     */
    fun sendEventReminder(
        context: Context,
        eventName: String,
        daysUntilEvent: Int,
        notificationManager: NotificationManager
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming Event")
            .setContentText("$eventName is in $daysUntilEvent days! Time to plan your outfit.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(context))
            .build()
        
        notificationManager.notify(1006, notification)
    }
    
    /**
     * Create pending intent to open app.
     */
    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    /**
     * Cancel all notifications.
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }
}
