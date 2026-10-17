package com.fabricfit.app.data.cache

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Cache entity for offline posts.
 */
@Entity(tableName = "cached_posts")
data class CachedPost(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val mediaType: String,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
    val cachedAt: Long = System.currentTimeMillis(),
)

/**
 * DAO for cached posts.
 */
@Dao
interface CachedPostDao {
    @Query("SELECT * FROM cached_posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CachedPost>>
    
    @Query("SELECT * FROM cached_posts WHERE id = :postId")
    suspend fun getById(postId: String): CachedPost?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: CachedPost)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<CachedPost>)
    
    @Query("DELETE FROM cached_posts WHERE id = :postId")
    suspend fun delete(postId: String)
    
    @Query("DELETE FROM cached_posts")
    suspend fun deleteAll()
    
    @Query("DELETE FROM cached_posts WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

/**
 * Database for offline cache.
 */
@Database(entities = [CachedPost::class], version = 1, exportSchema = false)
abstract class OfflineCacheDatabase : RoomDatabase() {
    abstract fun cachedPostDao(): CachedPostDao
    
    companion object {
        @Volatile
        private var INSTANCE: OfflineCacheDatabase? = null
        
        fun getInstance(context: Context): OfflineCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineCacheDatabase::class.java,
                    "offline_cache_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Manager for offline data caching.
 */
class OfflineCacheManager(context: Context) {
    
    private val db = OfflineCacheDatabase.getInstance(context)
    private val dao = db.cachedPostDao()
    
    /**
     * Cache posts for offline viewing.
     */
    suspend fun cachePosts(posts: List<CachedPost>) {
        dao.insertAll(posts)
    }
    
    /**
     * Get cached posts.
     */
    fun observeCachedPosts(): Flow<List<CachedPost>> {
        return dao.observeAll()
    }
    
    /**
     * Get a specific cached post.
     */
    suspend fun getCachedPost(postId: String): CachedPost? {
        return dao.getById(postId)
    }
    
    /**
     * Update a cached post (e.g., after liking).
     */
    suspend fun updateCachedPost(post: CachedPost) {
        dao.insert(post)
    }
    
    /**
     * Clear old cache (older than 7 days).
     */
    suspend fun clearOldCache() {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        dao.deleteOlderThan(sevenDaysAgo)
    }
    
    /**
     * Clear all cache.
     */
    suspend fun clearAllCache() {
        dao.deleteAll()
    }
    
    /**
     * Check if a post is cached.
     */
    suspend fun isPostCached(postId: String): Boolean {
        return dao.getById(postId) != null
    }
}
