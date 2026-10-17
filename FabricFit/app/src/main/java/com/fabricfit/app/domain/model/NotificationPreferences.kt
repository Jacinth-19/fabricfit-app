package com.fabricfit.app.domain.model

/**
 * User preferences for push notifications.
 */
data class NotificationPreferences(
    val likesEnabled: Boolean = true,
    val commentsEnabled: Boolean = true,
    val followsEnabled: Boolean = true,
    val challengesEnabled: Boolean = true,
    val mentionsEnabled: Boolean = true,
    val emailNotifications: Boolean = false,
)
