package com.fabricfit.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val blockedAt: Long = System.currentTimeMillis(),
    val reason: String? = null,
)
