package com.fabricfit.app.domain.model

/**
 * Ephemeral story that expires after 24 hours.
 */
data class Story(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val mediaType: StoryMediaType,
    val mediaUrl: String,
    val caption: String? = null,
    val stickers: List<StorySticker> = emptyList(),
    val musicUrl: String? = null,
    val views: List<StoryView> = emptyList(),
    val createdAt: Long,
    val expiresAt: Long,
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
    
    val viewCount: Int
        get() = views.size
    
    val hasViewedBy: (String) -> Boolean = { userId ->
        views.any { it.userId == userId }
    }
}

enum class StoryMediaType {
    IMAGE,
    VIDEO,
}

/**
 * Record of a user viewing a story.
 */
data class StoryView(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val viewedAt: Long,
)

/**
 * Interactive sticker on a story.
 */
data class StorySticker(
    val id: String,
    val type: StickerType,
    val content: String,
    val positionX: Float,
    val positionY: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f,
)

enum class StickerType {
    TEXT,
    EMOJI,
    POLL,
    QUESTION,
    LOCATION,
    MENTION,
    HASHTAG,
    MUSIC,
}

/**
 * User's story collection (active stories).
 */
data class UserStories(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val stories: List<Story>,
    val hasUnviewedStories: Boolean,
) {
    val latestStory: Story?
        get() = stories.maxByOrNull { it.createdAt }
    
    val activeStoryCount: Int
        get() = stories.count { !it.isExpired }
}

/**
 * Story creation draft.
 */
data class StoryDraft(
    val mediaUri: String,
    val mediaType: StoryMediaType,
    val caption: String? = null,
    val stickers: List<StorySticker> = emptyList(),
    val musicUrl: String? = null,
)
