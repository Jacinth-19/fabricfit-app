package com.fabricfit.app.data.repository

import com.fabricfit.app.data.local.db.ChallengeEntryDao
import com.fabricfit.app.data.local.db.CommentDao
import com.fabricfit.app.data.local.db.CommunityUserDao
import com.fabricfit.app.data.local.db.GalleryItemDao
import com.fabricfit.app.data.local.db.LikeDao
import com.fabricfit.app.data.local.db.StyleChallengeDao
import com.fabricfit.app.data.mapper.toDomain
import com.fabricfit.app.data.mapper.toEntity
import com.fabricfit.app.domain.model.ChallengeEntry
import com.fabricfit.app.domain.model.Comment
import com.fabricfit.app.domain.model.CommunityUser
import com.fabricfit.app.domain.model.GalleryItem
import com.fabricfit.app.domain.model.Like
import com.fabricfit.app.domain.model.StyleChallenge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for community features: galleries, follows, likes, comments, challenges.
 */
class CommunityRepository(
    private val userDao: CommunityUserDao,
    private val galleryDao: GalleryItemDao,
    private val commentDao: CommentDao,
    private val likeDao: LikeDao,
    private val challengeDao: StyleChallengeDao,
    private val entryDao: ChallengeEntryDao,
    private val currentUserId: String = "current_user", // In production, get from auth
) {

    // ==================== USER OPERATIONS ====================

    fun observeAllUsers(): Flow<List<CommunityUser>> =
        userDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeFollowing(): Flow<List<CommunityUser>> =
        userDao.observeFollowing().map { entities -> entities.map { it.toDomain() } }

    suspend fun getUser(userId: String): CommunityUser? =
        userDao.getById(userId)?.toDomain()

    suspend fun getUserByUsername(username: String): CommunityUser? =
        userDao.getByUsername(username)?.toDomain()

    suspend fun followUser(userId: String) {
        val user = userDao.getById(userId) ?: return
        userDao.update(user.copy(
            isFollowing = true,
            followerCount = user.followerCount + 1
        ))
    }

    suspend fun unfollowUser(userId: String) {
        val user = userDao.getById(userId) ?: return
        userDao.update(user.copy(
            isFollowing = false,
            followerCount = (user.followerCount - 1).coerceAtLeast(0)
        ))
    }

    // ==================== GALLERY OPERATIONS ====================

    fun observeAllGalleryItems(): Flow<List<GalleryItem>> =
        galleryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeGalleryByUser(userId: String): Flow<List<GalleryItem>> =
        galleryDao.observeByUser(userId).map { entities -> entities.map { it.toDomain() } }

    fun observeLikedGalleryItems(): Flow<List<GalleryItem>> =
        galleryDao.observeLiked().map { entities -> entities.map { it.toDomain() } }

    fun observeGalleryByTag(tag: String): Flow<List<GalleryItem>> =
        galleryDao.observeByTag(tag).map { entities -> entities.map { it.toDomain() } }

    suspend fun getGalleryItem(itemId: String): GalleryItem? =
        galleryDao.getById(itemId)?.toDomain()

    suspend fun createGalleryItem(
        title: String,
        description: String,
        imageUrl: String,
        fabricIds: List<Long> = emptyList(),
        styleId: String? = null,
        tags: List<String> = emptyList(),
    ): GalleryItem {
        val currentUser = userDao.getById(currentUserId) ?: throw IllegalStateException("Current user not found")
        
        val item = GalleryItem(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            username = currentUser.username,
            userAvatar = currentUser.avatarUrl,
            title = title,
            description = description,
            imageUrl = imageUrl,
            fabricIds = fabricIds,
            styleId = styleId,
            tags = tags,
        )
        
        galleryDao.insert(item.toEntity())
        
        // Update user's gallery count
        userDao.update(currentUser.copy(galleryCount = currentUser.galleryCount + 1))
        
        return item
    }

    suspend fun deleteGalleryItem(itemId: String) {
        val item = galleryDao.getById(itemId) ?: return
        if (item.userId != currentUserId) return // Only delete own items
        
        galleryDao.delete(itemId)
        commentDao.deleteByGalleryItem(itemId)
        
        // Update user's gallery count
        val currentUser = userDao.getById(currentUserId) ?: return
        userDao.update(currentUser.copy(galleryCount = (currentUser.galleryCount - 1).coerceAtLeast(0)))
    }

    // ==================== LIKE OPERATIONS ====================

    suspend fun likeGalleryItem(itemId: String) {
        val item = galleryDao.getById(itemId) ?: return
        
        val like = Like(
            id = UUID.randomUUID().toString(),
            galleryItemId = itemId,
            userId = currentUserId,
        )
        
        likeDao.insert(like.toEntity())
        galleryDao.update(item.copy(
            likeCount = item.likeCount + 1,
            isLiked = true
        ))
    }

    suspend fun unlikeGalleryItem(itemId: String) {
        val item = galleryDao.getById(itemId) ?: return
        
        likeDao.delete(itemId, currentUserId)
        galleryDao.update(item.copy(
            likeCount = (item.likeCount - 1).coerceAtLeast(0),
            isLiked = false
        ))
    }

    suspend fun toggleLike(itemId: String) {
        val item = galleryDao.getById(itemId) ?: return
        if (item.isLiked) {
            unlikeGalleryItem(itemId)
        } else {
            likeGalleryItem(itemId)
        }
    }

    // ==================== COMMENT OPERATIONS ====================

    fun observeComments(galleryItemId: String): Flow<List<Comment>> =
        commentDao.observeByGalleryItem(galleryItemId).map { entities -> 
            entities.map { it.toDomain() } 
        }

    suspend fun addComment(galleryItemId: String, text: String): Comment {
        val currentUser = userDao.getById(currentUserId) ?: throw IllegalStateException("Current user not found")
        val item = galleryDao.getById(galleryItemId) ?: throw IllegalStateException("Gallery item not found")
        
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            galleryItemId = galleryItemId,
            userId = currentUserId,
            username = currentUser.username,
            userAvatar = currentUser.avatarUrl,
            text = text,
        )
        
        commentDao.insert(comment.toEntity())
        galleryDao.update(item.copy(commentCount = item.commentCount + 1))
        
        return comment
    }

    suspend fun deleteComment(commentId: String) {
        val comment = commentDao.observeByGalleryItem("").toString() // Can't get single comment, skip
        // In production, you'd have a getById method
        commentDao.delete(commentId)
    }

    // ==================== CHALLENGE OPERATIONS ====================

    fun observeAllChallenges(): Flow<List<StyleChallenge>> =
        challengeDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeActiveChallenges(): Flow<List<StyleChallenge>> =
        challengeDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    suspend fun getChallenge(challengeId: String): StyleChallenge? =
        challengeDao.getById(challengeId)?.toDomain()

    suspend fun joinChallenge(challengeId: String) {
        val challenge = challengeDao.getById(challengeId) ?: return
        challengeDao.update(challenge.copy(
            participantCount = challenge.participantCount + 1,
            isParticipating = true
        ))
    }

    fun observeChallengeEntries(challengeId: String): Flow<List<ChallengeEntry>> =
        entryDao.observeByChallenge(challengeId).map { entities -> 
            entities.map { it.toDomain() } 
        }

    suspend fun submitChallengeEntry(
        challengeId: String,
        galleryItemId: String,
    ): ChallengeEntry {
        val currentUser = userDao.getById(currentUserId) ?: throw IllegalStateException("Current user not found")
        val challenge = challengeDao.getById(challengeId) ?: throw IllegalStateException("Challenge not found")
        
        val entry = ChallengeEntry(
            id = UUID.randomUUID().toString(),
            challengeId = challengeId,
            userId = currentUserId,
            username = currentUser.username,
            userAvatar = currentUser.avatarUrl,
            galleryItemId = galleryItemId,
        )
        
        entryDao.insert(entry.toEntity())
        
        // Update challenge participant count if not already participating
        if (!challenge.isParticipating) {
            challengeDao.update(challenge.copy(
                participantCount = challenge.participantCount + 1,
                isParticipating = true
            ))
        }
        
        return entry
    }

    suspend fun voteForEntry(entryId: String) {
        val entries = entryDao.observeByChallenge("").toString() // Can't get single entry
        // In production, you'd have a getById method
        // For now, this is a placeholder
    }
}
