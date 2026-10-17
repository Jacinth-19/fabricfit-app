package com.fabricfit.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedUserDao {
    @Query("SELECT * FROM blocked_users ORDER BY blockedAt DESC")
    fun observeAll(): Flow<List<BlockedUserEntity>>

    @Query("SELECT * FROM blocked_users WHERE userId = :userId")
    suspend fun getById(userId: String): BlockedUserEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_users WHERE userId = :userId)")
    suspend fun isBlocked(userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM blocked_users")
    suspend fun deleteAll()
}
