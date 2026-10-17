package com.fabricfit.app.data.cache

import com.fabricfit.app.domain.model.GalleryItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineCacheManagerTest {

    private lateinit var cacheManager: OfflineCacheManager

    @Before
    fun setup() {
        cacheManager = OfflineCacheManager()
        runBlocking {
            cacheManager.clearAllCache()
        }
    }

    @Test
    fun `cacheItem stores item correctly`() = runBlocking {
        val item = createTestItem("item_1")
        cacheManager.cacheItem(item)

        val cached = cacheManager.getCachedItem("item_1")
        assertNotNull(cached)
        assertEquals("item_1", cached!!.id)
    }

    @Test
    fun `cacheItems stores multiple items`() = runBlocking {
        val items = listOf(
            createTestItem("item_1"),
            createTestItem("item_2"),
            createTestItem("item_3")
        )

        cacheManager.cacheItems(items)

        items.forEach { item ->
            val cached = cacheManager.getCachedItem(item.id)
            assertNotNull(cached)
            assertEquals(item.id, cached!!.id)
        }
    }

    @Test
    fun `getCachedItem returns null for non-existent item`() = runBlocking {
        val cached = cacheManager.getCachedItem("nonexistent")
        assertNull(cached)
    }

    @Test
    fun `observeCachedItems returns all cached items`() = runBlocking {
        val items = listOf(
            createTestItem("item_1"),
            createTestItem("item_2")
        )

        cacheManager.cacheItems(items)

        val cachedItems = cacheManager.observeCachedItems().first()
        assertEquals(2, cachedItems.size)
    }

    @Test
    fun `deleteCachedItem removes item`() = runBlocking {
        val item = createTestItem("item_1")
        cacheManager.cacheItem(item)

        cacheManager.deleteCachedItem("item_1")

        val cached = cacheManager.getCachedItem("item_1")
        assertNull(cached)
    }

    @Test
    fun `clearAllCache removes all items`() = runBlocking {
        val items = listOf(
            createTestItem("item_1"),
            createTestItem("item_2"),
            createTestItem("item_3")
        )

        cacheManager.cacheItems(items)
        cacheManager.clearAllCache()

        val cachedItems = cacheManager.observeCachedItems().first()
        assertTrue(cachedItems.isEmpty())
    }

    @Test
    fun `getCachedItemsCount returns correct count`() = runBlocking {
        val items = listOf(
            createTestItem("item_1"),
            createTestItem("item_2")
        )

        cacheManager.cacheItems(items)

        val count = cacheManager.getCachedItemsCount()
        assertEquals(2, count)
    }

    @Test
    fun `getCachedItemsCount returns zero when empty`() = runBlocking {
        val count = cacheManager.getCachedItemsCount()
        assertEquals(0, count)
    }

    @Test
    fun `cacheItem updates existing item`() = runBlocking {
        val item1 = createTestItem("item_1", "Original Title")
        cacheManager.cacheItem(item1)

        val item2 = createTestItem("item_1", "Updated Title")
        cacheManager.cacheItem(item2)

        val cached = cacheManager.getCachedItem("item_1")
        assertNotNull(cached)
        assertEquals("Updated Title", cached!!.title)
    }

    @Test
    fun `cacheItem preserves item properties`() = runBlocking {
        val item = GalleryItem(
            id = "item_1",
            userId = "user_1",
            title = "Test Title",
            description = "Test Description",
            imageUrl = "https://example.com/image.jpg",
            videoUrl = null,
            mediaType = com.fabricfit.app.domain.model.MediaType.IMAGE,
            fabricIds = listOf("fabric_1", "fabric_2"),
            styleId = "style_1",
            likeCount = 10,
            commentCount = 5,
            isLiked = true,
            createdAt = System.currentTimeMillis()
        )

        cacheManager.cacheItem(item)

        val cached = cacheManager.getCachedItem("item_1")
        assertNotNull(cached)
        assertEquals(item.title, cached!!.title)
        assertEquals(item.description, cached.description)
        assertEquals(item.imageUrl, cached.imageUrl)
        assertEquals(item.fabricIds, cached.fabricIds)
        assertEquals(item.styleId, cached.styleId)
        assertEquals(item.likeCount, cached.likeCount)
        assertEquals(item.commentCount, cached.commentCount)
        assertEquals(item.isLiked, cached.isLiked)
    }

    @Test
    fun `observeCachedItems emits updates when cache changes`() = runBlocking {
        val item1 = createTestItem("item_1")
        cacheManager.cacheItem(item1)

        val initialItems = cacheManager.observeCachedItems().first()
        assertEquals(1, initialItems.size)

        val item2 = createTestItem("item_2")
        cacheManager.cacheItem(item2)

        val updatedItems = cacheManager.observeCachedItems().first()
        assertEquals(2, updatedItems.size)
    }

    @Test
    fun `deleteCachedItem does nothing for non-existent item`() = runBlocking {
        cacheManager.deleteCachedItem("nonexistent")
        // Should not throw exception
    }

    @Test
    fun `cache handles empty list`() = runBlocking {
        cacheManager.cacheItems(emptyList())
        val count = cacheManager.getCachedItemsCount()
        assertEquals(0, count)
    }

    @Test
    fun `cached items maintain order`() = runBlocking {
        val items = listOf(
            createTestItem("item_1", createdAt = 1000L),
            createTestItem("item_2", createdAt = 2000L),
            createTestItem("item_3", createdAt = 3000L)
        )

        cacheManager.cacheItems(items)

        val cachedItems = cacheManager.observeCachedItems().first()
        assertEquals(3, cachedItems.size)
        // Items should be ordered by creation time (newest first)
        assertTrue(cachedItems[0].createdAt >= cachedItems[1].createdAt)
        assertTrue(cachedItems[1].createdAt >= cachedItems[2].createdAt)
    }

    private fun createTestItem(
        id: String,
        title: String = "Test Item",
        createdAt: Long = System.currentTimeMillis()
    ): GalleryItem {
        return GalleryItem(
            id = id,
            userId = "user_1",
            title = title,
            description = "Test Description",
            imageUrl = "https://example.com/image.jpg",
            videoUrl = null,
            mediaType = com.fabricfit.app.domain.model.MediaType.IMAGE,
            fabricIds = emptyList(),
            styleId = null,
            likeCount = 0,
            commentCount = 0,
            isLiked = false,
            createdAt = createdAt
        )
    }
}
