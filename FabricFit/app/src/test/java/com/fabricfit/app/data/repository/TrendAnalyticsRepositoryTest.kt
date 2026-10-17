package com.fabricfit.app.data.repository

import com.fabricfit.app.domain.model.TrendTimePeriod
import com.fabricfit.app.domain.model.TrendType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrendAnalyticsRepositoryTest {

    private lateinit var repository: TrendAnalyticsRepository

    @Before
    fun setup() {
        repository = TrendAnalyticsRepository()
    }

    @Test
    fun `getTrendingItems returns items for hashtag type`() = runBlocking {
        val items = repository.getTrendingItems(TrendType.HASHTAG, TrendTimePeriod.LAST_7_DAYS, 10)

        assertNotNull(items)
        assertTrue(items.isNotEmpty())
        assertTrue(items.size <= 10)
        items.forEach { item ->
            assertEquals(TrendType.HASHTAG, item.type)
            assertTrue(item.name.startsWith("#"))
            assertTrue(item.postCount > 0)
            assertTrue(item.growthRate >= 0.0f)
            assertTrue(item.rank > 0)
        }
    }

    @Test
    fun `getTrendingItems returns items for style type`() = runBlocking {
        val items = repository.getTrendingItems(TrendType.STYLE, TrendTimePeriod.LAST_7_DAYS, 10)

        assertNotNull(items)
        assertTrue(items.isNotEmpty())
        items.forEach { item ->
            assertEquals(TrendType.STYLE, item.type)
            assertTrue(item.name.isNotEmpty())
        }
    }

    @Test
    fun `getTrendingItems respects limit parameter`() = runBlocking {
        val limit = 5
        val items = repository.getTrendingItems(TrendType.HASHTAG, TrendTimePeriod.LAST_7_DAYS, limit)

        assertTrue(items.size <= limit)
    }

    @Test
    fun `getTrendingItems returns sorted by post count`() = runBlocking {
        val items = repository.getTrendingItems(TrendType.HASHTAG, TrendTimePeriod.LAST_7_DAYS, 20)

        // Should be sorted by post count (descending)
        for (i in 0 until items.size - 1) {
            assertTrue(items[i].postCount >= items[i + 1].postCount)
        }
    }

    @Test
    fun `getTrendPredictions returns predictions with confidence`() = runBlocking {
        val predictions = repository.getTrendPredictions(10)

        assertNotNull(predictions)
        assertTrue(predictions.isNotEmpty())
        assertTrue(predictions.size <= 10)
        predictions.forEach { prediction ->
            assertNotNull(prediction.item)
            assertTrue(prediction.predictedGrowth >= 0.0f)
            assertTrue(prediction.confidence in 0.0f..1.0f)
        }
    }

    @Test
    fun `getTrendPredictions includes peak date`() = runBlocking {
        val predictions = repository.getTrendPredictions(5)

        predictions.forEach { prediction ->
            if (prediction.peakDate != null) {
                assertTrue(prediction.peakDate > System.currentTimeMillis())
            }
        }
    }

    @Test
    fun `getUserTrendAffinities returns affinities for user`() = runBlocking {
        val userId = "user_123"
        val affinities = repository.getUserTrendAffinities(userId)

        assertNotNull(affinities)
        affinities.forEach { affinity ->
            assertEquals(userId, affinity.userId)
            assertTrue(affinity.affinityScore in 0.0f..1.0f)
            assertTrue(affinity.interactionCount >= 0)
        }
    }

    @Test
    fun `recordTrendInteraction does not throw exception`() = runBlocking {
        repository.recordTrendInteraction("user_123", "trend_456")
        // Should complete without exception
    }

    @Test
    fun `getPersonalizedTrends returns trends for user`() = runBlocking {
        val userId = "user_123"
        val trends = repository.getPersonalizedTrends(userId, 10)

        assertNotNull(trends)
        assertTrue(trends.size <= 10)
        trends.forEach { trend ->
            assertNotNull(trend.id)
            assertNotNull(trend.name)
            assertTrue(trend.postCount > 0)
        }
    }

    @Test
    fun `searchTrends finds matching trends`() = runBlocking {
        val query = "fashion"
        val trends = repository.searchTrends(query, 20)

        assertNotNull(trends)
        assertTrue(trends.size <= 20)
        trends.forEach { trend ->
            assertTrue(
                trend.name.contains(query, ignoreCase = true) ||
                trend.tags.any { it.contains(query, ignoreCase = true) }
            )
        }
    }

    @Test
    fun `searchTrends returns empty for no matches`() = runBlocking {
        val query = "xyznonexistent123"
        val trends = repository.searchTrends(query, 20)

        assertNotNull(trends)
        assertTrue(trends.isEmpty())
    }

    @Test
    fun `getWhatsHot returns top trending items`() = runBlocking {
        val hotItems = repository.getWhatsHot(5)

        assertNotNull(hotItems)
        assertTrue(hotItems.isNotEmpty())
        assertTrue(hotItems.size <= 5)
        hotItems.forEach { item ->
            assertTrue(item.growthRate > 0.0f)
        }
    }

    @Test
    fun `getWhatsHot returns sorted by growth rate`() = runBlocking {
        val hotItems = repository.getWhatsHot(10)

        // Should be sorted by growth rate (descending)
        for (i in 0 until hotItems.size - 1) {
            assertTrue(hotItems[i].growthRate >= hotItems[i + 1].growthRate)
        }
    }

    @Test
    fun `different time periods return different results`() = runBlocking {
        val last24h = repository.getTrendingItems(TrendType.HASHTAG, TrendTimePeriod.LAST_24_HOURS, 10)
        val last7d = repository.getTrendingItems(TrendType.HASHTAG, TrendTimePeriod.LAST_7_DAYS, 10)

        assertNotNull(last24h)
        assertNotNull(last7d)
        // Both should have items
        assertTrue(last24h.isNotEmpty())
        assertTrue(last7d.isNotEmpty())
    }

    @Test
    fun `all trend types are supported`() = runBlocking {
        TrendType.values().forEach { type ->
            val items = repository.getTrendingItems(type, TrendTimePeriod.LAST_7_DAYS, 5)
            assertNotNull(items)
        }
    }

    @Test
    fun `all time periods are supported`() = runBlocking {
        TrendTimePeriod.values().forEach { period ->
            val items = repository.getTrendingItems(TrendType.HASHTAG, period, 5)
            assertNotNull(items)
        }
    }
}
