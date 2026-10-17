package com.fabricfit.app.domain.model

/**
 * Community user profile for social features.
 */
data class CommunityUser(
    val id: String,
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

/**
 * Media type for gallery items.
 */
enum class MediaType {
    IMAGE,
    VIDEO,
}

/**
 * User-generated style gallery item.
 */
data class GalleryItem(
    val id: String,
    val userId: String,
    val username: String,
    val userAvatar: String? = null,
    val title: String,
    val description: String = "",
    val imageUrl: String,
    val mediaType: MediaType = MediaType.IMAGE,
    val videoUrl: String? = null,
    val videoDuration: Long? = null,
    val thumbnailUrl: String? = null,
    val fabricIds: List<Long> = emptyList(),
    val styleId: String? = null,
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Comment on a gallery item.
 */
data class Comment(
    val id: String,
    val galleryItemId: String,
    val userId: String,
    val username: String,
    val userAvatar: String? = null,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Like on a gallery item.
 */
data class Like(
    val id: String,
    val galleryItemId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Style challenge for community engagement.
 */
data class StyleChallenge(
    val id: String,
    val title: String,
    val description: String,
    val bannerUrl: String? = null,
    val startDate: Long,
    val endDate: Long,
    val participantCount: Int = 0,
    val requiredFabricTypes: List<FabricCategory> = emptyList(),
    val requiredOccasions: List<Occasion> = emptyList(),
    val prizes: List<String> = emptyList(),
    val isActive: Boolean = false,
    val isParticipating: Boolean = false,
)

/**
 * Challenge entry submitted by a user.
 */
data class ChallengeEntry(
    val id: String,
    val challengeId: String,
    val userId: String,
    val username: String,
    val userAvatar: String? = null,
    val galleryItemId: String,
    val submissionDate: Long = System.currentTimeMillis(),
    val voteCount: Int = 0,
    val isVoted: Boolean = false,
)
