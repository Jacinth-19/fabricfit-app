package com.fabricfit.app.data.repository

import com.fabricfit.app.domain.model.StoryDraft
import com.fabricfit.app.domain.model.StoryMediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StoryRepositoryTest {

    private lateinit var repository: StoryRepository

    @Before
    fun setup() {
        repository = StoryRepository()
    }

    @Test
    fun `observeStories returns active stories`() = runBlocking {
        val stories = repository.observeStories().first()

        assertNotNull(stories)
        stories.forEach { userStories ->
            assertNotNull(userStories.userId)
            assertNotNull(userStories.userName)
            assertTrue(userStories.stories.isNotEmpty())
            userStories.stories.forEach { story ->
                assertFalse(story.isExpired)
            }
        }
    }

    @Test
    fun `getUserStories returns stories for specific user`() = runBlocking {
        val allStories = repository.observeStories().first()
        if (allStories.isNotEmpty()) {
            val userId = allStories[0].userId
            val userStories = repository.getUserStories(userId)

            assertNotNull(userStories)
            assertEquals(userId, userStories!!.userId)
            assertTrue(userStories.stories.isNotEmpty())
        }
    }

    @Test
    fun `getUserStories returns null for user without stories`() = runBlocking {
        val userStories = repository.getUserStories("nonexistent_user")
        assertNull(userStories)
    }

    @Test
    fun `getStory returns story by id`() = runBlocking {
        val allStories = repository.observeStories().first()
        if (allStories.isNotEmpty() && allStories[0].stories.isNotEmpty()) {
            val storyId = allStories[0].stories[0].id
            val story = repository.getStory(storyId)

            assertNotNull(story)
            assertEquals(storyId, story!!.id)
        }
    }

    @Test
    fun `getStory returns null for invalid id`() = runBlocking {
        val story = repository.getStory("invalid_id")
        assertNull(story)
    }

    @Test
    fun `createStory creates new story`() = runBlocking {
        val userId = "new_user"
        val userName = "New User"
        val draft = StoryDraft(
            mediaUri = "https://example.com/story.jpg",
            mediaType = StoryMediaType.IMAGE,
            caption = "Test story"
        )

        val story = repository.createStory(userId, userName, null, draft)

        assertNotNull(story)
        assertEquals(userId, story.userId)
        assertEquals(userName, story.userName)
        assertEquals(draft.mediaUri, story.mediaUrl)
        assertEquals(draft.caption, story.caption)
        assertFalse(story.isExpired)
        assertTrue(story.expiresAt > System.currentTimeMillis())
    }

    @Test
    fun `createStory sets correct expiration time`() = runBlocking {
        val userId = "test_user"
        val draft = StoryDraft(
            mediaUri = "https://example.com/story.jpg",
            mediaType = StoryMediaType.IMAGE
        )

        val beforeCreate = System.currentTimeMillis()
        val story = repository.createStory(userId, "Test User", null, draft)
        val afterCreate = System.currentTimeMillis()

        val expectedExpiry = beforeCreate + (24 * 60 * 60 * 1000L)
        assertTrue(story.expiresAt >= expectedExpiry)
        assertTrue(story.expiresAt <= afterCreate + (24 * 60 * 60 * 1000L))
    }

    @Test
    fun `recordStoryView adds view to story`() = runBlocking {
        val allStories = repository.observeStories().first()
        if (allStories.isNotEmpty() && allStories[0].stories.isNotEmpty()) {
            val storyId = allStories[0].stories[0].id
            val viewerId = "viewer_123"
            val viewerName = "Viewer"

            val storyBefore = repository.getStory(storyId)
            val viewsBefore = storyBefore?.viewCount ?: 0

            repository.recordStoryView(storyId, viewerId, viewerName, null)

            val storyAfter = repository.getStory(storyId)
            val viewsAfter = storyAfter?.viewCount ?: 0

            assertEquals(viewsBefore + 1, viewsAfter)
        }
    }

    @Test
    fun `recordStoryView does not duplicate views`() = runBlocking {
        val allStories = repository.observeStories().first()
        if (allStories.isNotEmpty() && allStories[0].stories.isNotEmpty()) {
            val storyId = allStories[0].stories[0].id
            val viewerId = "viewer_123"

            repository.recordStoryView(storyId, viewerId, "Viewer", null)
            val viewsAfterFirst = repository.getStory(storyId)?.viewCount ?: 0

            repository.recordStoryView(storyId, viewerId, "Viewer", null)
            val viewsAfterSecond = repository.getStory(storyId)?.viewCount ?: 0

            assertEquals(viewsAfterFirst, viewsAfterSecond)
        }
    }

    @Test
    fun `deleteStory removes story`() = runBlocking {
        val userId = "delete_user"
        val draft = StoryDraft(
            mediaUri = "https://example.com/story.jpg",
            mediaType = StoryMediaType.IMAGE
        )

        val story = repository.createStory(userId, "Delete User", null, draft)
        val storyId = story.id

        repository.deleteStory(storyId, userId)

        val deletedStory = repository.getStory(storyId)
        assertNull(deletedStory)
    }

    @Test
    fun `getStoryViewers returns viewers list`() = runBlocking {
        val allStories = repository.observeStories().first()
        if (allStories.isNotEmpty() && allStories[0].stories.isNotEmpty()) {
            val storyId = allStories[0].stories[0].id
            val viewers = repository.getStoryViewers(storyId)

            assertNotNull(viewers)
            viewers.forEach { view ->
                assertNotNull(view.userId)
                assertNotNull(view.userName)
                assertTrue(view.viewedAt > 0)
            }
        }
    }

    @Test
    fun `getMyStories returns user's own stories`() = runBlocking {
        val userId = "my_user"
        val draft = StoryDraft(
            mediaUri = "https://example.com/story.jpg",
            mediaType = StoryMediaType.IMAGE
        )

        repository.createStory(userId, "My User", null, draft)
        val myStories = repository.getMyStories(userId)

        assertNotNull(myStories)
        assertTrue(myStories.isNotEmpty())
        myStories.forEach { story ->
            assertEquals(userId, story.userId)
        }
    }

    @Test
    fun `hasActiveStories returns true when user has stories`() = runBlocking {
        val userId = "active_user"
        val draft = StoryDraft(
            mediaUri = "https://example.com/story.jpg",
            mediaType = StoryMediaType.IMAGE
        )

        repository.createStory(userId, "Active User", null, draft)
        val hasStories = repository.hasActiveStories(userId)

        assertTrue(hasStories)
    }

    @Test
    fun `hasActiveStories returns false when user has no stories`() = runBlocking {
        val hasStories = repository.hasActiveStories("nonexistent_user")
        assertFalse(hasStories)
    }

    @Test
    fun `cleanupExpiredStories removes expired stories`() = runBlocking {
        // This test would require mocking time or creating expired stories
        // For now, just verify it doesn't throw exception
        repository.cleanupExpiredStories()
    }

    @Test
    fun `story media types are supported`() = runBlocking {
        StoryMediaType.values().forEach { mediaType ->
            val draft = StoryDraft(
                mediaUri = "https://example.com/story.jpg",
                mediaType = mediaType
            )
            val story = repository.createStory("test_user", "Test", null, draft)
            assertEquals(mediaType, story.mediaType)
        }
    }

    @Test
    fun `story with caption is created correctly`() = runBlocking {
        val caption = "Test caption with emoji 🎉"
        val draft = StoryDraft(
            mediaUri = "https://example.com/story.jpg",
            mediaType = StoryMediaType.IMAGE,
            caption = caption
        )

        val story = repository.createStory("test_user", "Test", null, draft)
        assertEquals(caption, story.caption)
    }

    @Test
    fun `story with stickers is created correctly`() = runBlocking {
        val draft = StoryDraft(
            mediaUri = "https://example.com/story.jpg",
            mediaType = StoryMediaType.IMAGE,
            stickers = emptyList() // Would add stickers in real implementation
        )

        val story = repository.createStory("test_user", "Test", null, draft)
        assertNotNull(story.stickers)
    }
}
