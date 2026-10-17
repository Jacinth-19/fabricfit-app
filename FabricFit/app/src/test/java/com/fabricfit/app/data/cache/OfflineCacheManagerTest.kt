package com.fabricfit.app.data.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises [OfflineCacheManager] against a Robolectric application context —
 * it owns a Room database, so it needs a real (shadowed) Android context.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineCacheManagerTest {

    private lateinit var cacheManager: OfflineCacheManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        cacheManager = OfflineCacheManager(context)
        // The database is a process-wide singleton, so start from empty.
        runBlocking { cacheManager.clearAllCache() }
    }

    @Test
    fun `cachePosts stores a post`() = runBlocking {
        cacheManager.cachePosts(listOf(createTestPost("post_1")))

        val cached = cacheManager.getCachedPost("post_1")
        assertNotNull(cached)
        assertEquals("post_1", cached!!.id)
    }

    @Test
    fun `cachePosts stores multiple posts`() = runBlocking {
        val posts = listOf(
            createTestPost("post_1"),
            createTestPost("post_2"),
            createTestPost("post_3"),
        )

        cacheManager.cachePosts(posts)

        posts.forEach { post ->
            val cached = cacheManager.getCachedPost(post.id)
            assertNotNull(cached)
            assertEquals(post.id, cached!!.id)
        }
    }

    @Test
    fun `getCachedPost returns null for a post that was never cached`() = runBlocking {
        assertNull(cacheManager.getCachedPost("missing"))
    }

    @Test
    fun `observeCachedPosts emits every cached post`() = runBlocking {
        cacheManager.cachePosts(listOf(createTestPost("post_1"), createTestPost("post_2")))

        assertEquals(2, cacheManager.observeCachedPosts().first().size)
    }

    @Test
    fun `updateCachedPost replaces the stored copy`() = runBlocking {
        cacheManager.cachePosts(listOf(createTestPost("post_1")))
        cacheManager.updateCachedPost(createTestPost("post_1", title = "Updated title"))

        assertEquals("Updated title", cacheManager.getCachedPost("post_1")?.title)
    }

    @Test
    fun `isPostCached reflects what is stored`() = runBlocking {
        cacheManager.cachePosts(listOf(createTestPost("post_1")))

        assertTrue(cacheManager.isPostCached("post_1"))
        assertFalse(cacheManager.isPostCached("post_2"))
    }

    @Test
    fun `clearAllCache empties the cache`() = runBlocking {
        cacheManager.cachePosts(listOf(createTestPost("post_1"), createTestPost("post_2")))

        cacheManager.clearAllCache()

        assertEquals(0, cacheManager.observeCachedPosts().first().size)
    }

    @Test
    fun `clearOldCache only drops stale posts`() = runBlocking {
        val now = System.currentTimeMillis()
        cacheManager.cachePosts(
            listOf(
                createTestPost("stale", createdAt = 0L),
                createTestPost("fresh", createdAt = now),
            ),
        )

        cacheManager.clearOldCache()

        assertNull(cacheManager.getCachedPost("stale"))
        assertNotNull(cacheManager.getCachedPost("fresh"))
    }

    private fun createTestPost(
        id: String,
        title: String = "Post $id",
        createdAt: Long = System.currentTimeMillis(),
    ) = CachedPost(
        id = id,
        userId = "user_$id",
        title = title,
        description = "Description for $id",
        imageUrl = null,
        videoUrl = null,
        mediaType = "IMAGE",
        likeCount = 0,
        commentCount = 0,
        isLiked = false,
        createdAt = createdAt,
        cachedAt = createdAt,
    )
}
