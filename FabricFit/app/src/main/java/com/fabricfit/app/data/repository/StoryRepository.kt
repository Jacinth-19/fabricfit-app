package com.fabricfit.app.data.repository

import com.fabricfit.app.domain.model.Story
import com.fabricfit.app.domain.model.StoryDraft
import com.fabricfit.app.domain.model.StoryMediaType
import com.fabricfit.app.domain.model.StorySticker
import com.fabricfit.app.domain.model.StoryView
import com.fabricfit.app.domain.model.UserStories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Repository for managing ephemeral stories.
 */
class StoryRepository {
    
    // In-memory cache (replace with actual database/API in production)
    private val stories = mutableMapOf<String, MutableList<Story>>()
    private val storyDuration = 24 * 60 * 60 * 1000L // 24 hours
    
    init {
        // Initialize with mock data
        initializeMockData()
    }
    
    /**
     * Get all active stories from followed users.
     */
    fun observeStories(): Flow<List<UserStories>> = flow {
        val userStoriesMap = mutableMapOf<String, UserStories>()
        
        stories.forEach { (userId, userStoryList) ->
            val activeStories = userStoryList.filter { !it.isExpired }
            if (activeStories.isNotEmpty()) {
                val firstStory = activeStories.first()
                userStoriesMap[userId] = UserStories(
                    userId = userId,
                    userName = firstStory.userName,
                    userAvatarUrl = firstStory.userAvatarUrl,
                    stories = activeStories,
                    hasUnviewedStories = activeStories.any { story ->
                        !story.hasViewedBy("current_user_id") // Would use actual user ID
                    }
                )
            }
        }
        
        emit(userStoriesMap.values.toList())
    }
    
    /**
     * Get a specific user's stories.
     */
    suspend fun getUserStories(userId: String): UserStories? {
        val userStoryList = stories[userId]?.filter { !it.isExpired } ?: return null
        if (userStoryList.isEmpty()) return null
        
        val firstStory = userStoryList.first()
        return UserStories(
            userId = userId,
            userName = firstStory.userName,
            userAvatarUrl = firstStory.userAvatarUrl,
            stories = userStoryList,
            hasUnviewedStories = userStoryList.any { story ->
                !story.hasViewedBy("current_user_id") // Would use actual user ID
            }
        )
    }
    
    /**
     * Get a specific story by ID.
     */
    suspend fun getStory(storyId: String): Story? {
        return stories.values.flatten().find { it.id == storyId && !it.isExpired }
    }
    
    /**
     * Create a new story.
     */
    suspend fun createStory(
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        draft: StoryDraft,
    ): Story {
        val now = System.currentTimeMillis()
        val story = Story(
            id = "story_${now}",
            userId = userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            mediaType = draft.mediaType,
            mediaUrl = draft.mediaUri,
            caption = draft.caption,
            stickers = draft.stickers,
            musicUrl = draft.musicUrl,
            views = emptyList(),
            createdAt = now,
            expiresAt = now + storyDuration,
        )
        
        stories.getOrPut(userId) { mutableListOf() }.add(story)
        return story
    }
    
    /**
     * Record a story view.
     */
    suspend fun recordStoryView(storyId: String, userId: String, userName: String, userAvatarUrl: String?) {
        val story = getStory(storyId) ?: return
        
        // Check if already viewed
        if (story.hasViewedBy(userId)) return
        
        val view = StoryView(
            userId = userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            viewedAt = System.currentTimeMillis(),
        )
        
        val updatedStory = story.copy(views = story.views + view)
        
        // Update story in list
        stories[story.userId]?.let { storyList ->
            val index = storyList.indexOfFirst { it.id == storyId }
            if (index != -1) {
                storyList[index] = updatedStory
            }
        }
    }
    
    /**
     * Delete a story.
     */
    suspend fun deleteStory(storyId: String, userId: String) {
        stories[userId]?.removeAll { it.id == storyId }
    }
    
    /**
     * Get story viewers.
     */
    suspend fun getStoryViewers(storyId: String): List<StoryView> {
        return getStory(storyId)?.views ?: emptyList()
    }
    
    /**
     * Clean up expired stories.
     */
    suspend fun cleanupExpiredStories() {
        stories.forEach { (userId, storyList) ->
            storyList.removeAll { it.isExpired }
        }
        // Remove empty user entries
        stories.entries.removeAll { it.value.isEmpty() }
    }
    
    /**
     * Get user's own stories.
     */
    suspend fun getMyStories(userId: String): List<Story> {
        return stories[userId]?.filter { !it.isExpired } ?: emptyList()
    }
    
    /**
     * Check if user has active stories.
     */
    suspend fun hasActiveStories(userId: String): Boolean {
        return stories[userId]?.any { !it.isExpired } ?: false
    }
    
    /**
     * Initialize mock data for demo.
     */
    private fun initializeMockData() {
        val now = System.currentTimeMillis()
        val mockUsers = listOf(
            Triple("user_1", "Jane Designer", "https://example.com/avatar1.jpg"),
            Triple("user_2", "Mike Stylist", "https://example.com/avatar2.jpg"),
            Triple("user_3", "Sarah Fashion", "https://example.com/avatar3.jpg"),
        )
        
        mockUsers.forEach { (userId, userName, avatarUrl) ->
            val userStories = mutableListOf<Story>()
            
            // Create 2-3 stories per user
            repeat(Random.nextInt(2, 4)) { index ->
                val createdAt = now - (index * 2 * 60 * 60 * 1000L) // 2 hours apart
                userStories.add(
                    Story(
                        id = "story_${userId}_$index",
                        userId = userId,
                        userName = userName,
                        userAvatarUrl = avatarUrl,
                        mediaType = if (Random.nextBoolean()) StoryMediaType.IMAGE else StoryMediaType.VIDEO,
                        mediaUrl = "https://example.com/story_${userId}_$index.jpg",
                        caption = listOf(
                            "Check out my new outfit! 👗",
                            "Behind the scenes 📸",
                            "Fashion week vibes ✨",
                            "New collection drop! 🔥",
                            null
                        ).random(),
                        stickers = if (Random.nextBoolean()) {
                            listOf(
                                StorySticker(
                                    id = "sticker_$index",
                                    type = com.fabricfit.app.domain.model.StickerType.TEXT,
                                    content = "#OOTD",
                                    positionX = 0.5f,
                                    positionY = 0.8f,
                                )
                            )
                        } else emptyList(),
                        views = List(Random.nextInt(10, 100)) { viewIndex ->
                            StoryView(
                                userId = "viewer_$viewIndex",
                                userName = "Viewer $viewIndex",
                                userAvatarUrl = null,
                                viewedAt = createdAt + (viewIndex * 60 * 1000L),
                            )
                        },
                        createdAt = createdAt,
                        expiresAt = createdAt + storyDuration,
                    )
                )
            }
            
            stories[userId] = userStories
        }
    }
}
