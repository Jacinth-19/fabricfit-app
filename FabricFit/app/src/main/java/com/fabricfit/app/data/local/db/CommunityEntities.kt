package com.fabricfit.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_users")
data class CommunityUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val galleryCount: Int = 0,
    val isFollowing: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "gallery_items")
data class GalleryItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val userAvatar: String? = null,
    val title: String,
    val description: String = "",
    val imageUrl: String,
    val fabricIds: String = "", // Comma-separated
    val styleId: String? = null,
    val tags: String = "", // Comma-separated
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val galleryItemId: String,
    val userId: String,
    val username: String,
    val userAvatar: String? = null,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey val id: String,
    val galleryItemId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "style_challenges")
data class StyleChallengeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val bannerUrl: String? = null,
    val startDate: Long,
    val endDate: Long,
    val participantCount: Int = 0,
    val requiredFabricTypes: String = "", // Comma-separated
    val requiredOccasions: String = "", // Comma-separated
    val prizes: String = "", // Comma-separated
    val isActive: Boolean = false,
    val isParticipating: Boolean = false,
)

@Entity(tableName = "challenge_entries")
data class ChallengeEntryEntity(
    @PrimaryKey val id: String,
    val challengeId: String,
    val userId: String,
    val username: String,
    val userAvatar: String? = null,
    val galleryItemId: String,
    val submissionDate: Long = System.currentTimeMillis(),
    val voteCount: Int = 0,
    val isVoted: Boolean = false,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val reportedUserId: String,
    val contentType: String,
    val contentId: String,
    val reportType: String,
    val description: String = "",
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolution: String? = null,
)
