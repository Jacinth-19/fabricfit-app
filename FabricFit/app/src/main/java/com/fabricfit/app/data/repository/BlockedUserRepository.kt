package com.fabricfit.app.data.repository

import com.fabricfit.app.data.local.db.BlockedUserDao
import com.fabricfit.app.data.local.db.BlockedUserEntity
import com.fabricfit.app.domain.model.BlockedUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing blocked users.
 */
class BlockedUserRepository(
    private val blockedUserDao: BlockedUserDao,
) {
    /**
     * Observe all blocked users.
     */
    fun observeBlockedUsers(): Flow<List<BlockedUser>> {
        return blockedUserDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Check if a user is blocked.
     */
    suspend fun isUserBlocked(userId: String): Boolean {
        return blockedUserDao.isBlocked(userId)
    }

    /**
     * Block a user.
     */
    suspend fun blockUser(user: BlockedUser) {
        blockedUserDao.insert(user.toEntity())
    }

    /**
     * Unblock a user.
     */
    suspend fun unblockUser(userId: String) {
        blockedUserDao.delete(userId)
    }

    /**
     * Get blocked user by ID.
     */
    suspend fun getBlockedUser(userId: String): BlockedUser? {
        return blockedUserDao.getById(userId)?.toDomain()
    }

    /**
     * Unblock all users.
     */
    suspend fun unblockAll() {
        blockedUserDao.deleteAll()
    }

    private fun BlockedUserEntity.toDomain(): BlockedUser {
        return BlockedUser(
            userId = userId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            blockedAt = blockedAt,
            reason = reason,
        )
    }

    private fun BlockedUser.toEntity(): BlockedUserEntity {
        return BlockedUserEntity(
            userId = userId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            blockedAt = blockedAt,
            reason = reason,
        )
    }
}
