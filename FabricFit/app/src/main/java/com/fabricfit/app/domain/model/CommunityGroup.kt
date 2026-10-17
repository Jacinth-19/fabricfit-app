package com.fabricfit.app.domain.model

/**
 * Community group for users with shared interests.
 */
data class CommunityGroup(
    val id: String,
    val name: String,
    val description: String,
    val category: GroupCategory,
    val coverImageUrl: String?,
    val memberCount: Int,
    val postCount: Int,
    val isPrivate: Boolean,
    val requiresApproval: Boolean,
    val createdAt: Long,
    val createdBy: String,
    val tags: List<String> = emptyList(),
    val rules: List<String> = emptyList(),
)

enum class GroupCategory {
    FASHION,
    VINTAGE,
    SUSTAINABLE,
    DIY,
    STREETWEAR,
    FORMAL,
    CASUAL,
    ACCESSORIES,
    SHOES,
    GENERAL,
}

/**
 * User's membership in a group.
 */
data class GroupMembership(
    val groupId: String,
    val userId: String,
    val role: GroupRole,
    val joinedAt: Long,
    val status: MembershipStatus,
)

enum class GroupRole {
    OWNER,
    ADMIN,
    MODERATOR,
    MEMBER,
}

enum class MembershipStatus {
    ACTIVE,
    PENDING,
    BANNED,
    LEFT,
}

/**
 * Post within a group.
 */
data class GroupPost(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isPinned: Boolean,
    val createdAt: Long,
)

/**
 * Group event.
 */
data class GroupEvent(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val location: String?,
    val startTime: Long,
    val endTime: Long,
    val attendeeCount: Int,
    val createdBy: String,
    val imageUrl: String?,
)

/**
 * Join request for private groups.
 */
data class JoinRequest(
    val id: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val message: String?,
    val requestedAt: Long,
    val status: RequestStatus,
)

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
}
