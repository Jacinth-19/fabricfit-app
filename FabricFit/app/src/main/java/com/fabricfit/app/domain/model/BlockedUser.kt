package com.fabricfit.app.domain.model

/**
 * Blocked user record.
 */
data class BlockedUser(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val blockedAt: Long = System.currentTimeMillis(),
    val reason: String? = null,
)
