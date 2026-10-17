package com.fabricfit.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityUserDao {
    @Query("SELECT * FROM community_users ORDER BY username ASC")
    fun observeAll(): Flow<List<CommunityUserEntity>>

    @Query("SELECT * FROM community_users ORDER BY username ASC")
    suspend fun getAll(): List<CommunityUserEntity>

    @Query("SELECT * FROM community_users WHERE id = :id")
    suspend fun getById(id: String): CommunityUserEntity?

    @Query("SELECT * FROM community_users WHERE username = :username")
    suspend fun getByUsername(username: String): CommunityUserEntity?

    @Query("SELECT * FROM community_users WHERE isFollowing = 1 ORDER BY username ASC")
    fun observeFollowing(): Flow<List<CommunityUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: CommunityUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<CommunityUserEntity>)

    @Update
    suspend fun update(user: CommunityUserEntity)

    @Query("DELETE FROM community_users WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface GalleryItemDao {
    @Query("SELECT * FROM gallery_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GalleryItemEntity>>

    @Query("SELECT * FROM gallery_items ORDER BY createdAt DESC")
    suspend fun getAll(): List<GalleryItemEntity>

    @Query("SELECT * FROM gallery_items WHERE id = :id")
    suspend fun getById(id: String): GalleryItemEntity?

    @Query("SELECT * FROM gallery_items WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<GalleryItemEntity>>

    @Query("SELECT * FROM gallery_items WHERE isLiked = 1 ORDER BY createdAt DESC")
    fun observeLiked(): Flow<List<GalleryItemEntity>>

    @Query("SELECT * FROM gallery_items WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun observeByTag(tag: String): Flow<List<GalleryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GalleryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GalleryItemEntity>)

    @Update
    suspend fun update(item: GalleryItemEntity)

    @Query("DELETE FROM gallery_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM gallery_items")
    suspend fun deleteAll()
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE galleryItemId = :galleryItemId ORDER BY createdAt ASC")
    fun observeByGalleryItem(galleryItemId: String): Flow<List<CommentEntity>>

    @Query("SELECT COUNT(*) FROM comments WHERE galleryItemId = :galleryItemId")
    suspend fun countByGalleryItem(galleryItemId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM comments WHERE galleryItemId = :galleryItemId")
    suspend fun deleteByGalleryItem(galleryItemId: String)
}

@Dao
interface LikeDao {
    @Query("SELECT * FROM likes WHERE galleryItemId = :galleryItemId")
    suspend fun getByGalleryItem(galleryItemId: String): List<LikeEntity>

    @Query("SELECT * FROM likes WHERE userId = :userId")
    fun observeByUser(userId: String): Flow<List<LikeEntity>>

    @Query("SELECT COUNT(*) FROM likes WHERE galleryItemId = :galleryItemId")
    suspend fun countByGalleryItem(galleryItemId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(like: LikeEntity)

    @Query("DELETE FROM likes WHERE galleryItemId = :galleryItemId AND userId = :userId")
    suspend fun delete(galleryItemId: String, userId: String)
}

@Dao
interface StyleChallengeDao {
    @Query("SELECT * FROM style_challenges ORDER BY startDate DESC")
    fun observeAll(): Flow<List<StyleChallengeEntity>>

    @Query("SELECT * FROM style_challenges ORDER BY startDate DESC")
    suspend fun getAll(): List<StyleChallengeEntity>

    @Query("SELECT * FROM style_challenges WHERE isActive = 1 ORDER BY endDate ASC")
    fun observeActive(): Flow<List<StyleChallengeEntity>>

    @Query("SELECT * FROM style_challenges WHERE id = :id")
    suspend fun getById(id: String): StyleChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: StyleChallengeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(challenges: List<StyleChallengeEntity>)

    @Update
    suspend fun update(challenge: StyleChallengeEntity)

    @Query("DELETE FROM style_challenges WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ChallengeEntryDao {
    @Query("SELECT * FROM challenge_entries WHERE challengeId = :challengeId ORDER BY voteCount DESC")
    fun observeByChallenge(challengeId: String): Flow<List<ChallengeEntryEntity>>

    @Query("SELECT * FROM challenge_entries WHERE userId = :userId ORDER BY submissionDate DESC")
    fun observeByUser(userId: String): Flow<List<ChallengeEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ChallengeEntryEntity)

    @Update
    suspend fun update(entry: ChallengeEntryEntity)

    @Query("DELETE FROM challenge_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 10")
    fun observeRecent(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun deleteAll()
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: String): ReportEntity?

    @Query("SELECT * FROM reports WHERE reporterId = :reporterId ORDER BY createdAt DESC")
    fun observeByReporter(reporterId: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE reportedUserId = :reportedUserId ORDER BY createdAt DESC")
    fun observeByReportedUser(reportedUserId: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE contentId = :contentId AND contentType = :contentType ORDER BY createdAt DESC")
    suspend fun getByContent(contentId: String, contentType: String): List<ReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity)

    @Update
    suspend fun update(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM reports WHERE contentId = :contentId AND contentType = :contentType AND status = 'PENDING'")
    suspend fun countPendingForContent(contentId: String, contentType: String): Int
}
