package com.fabricfit.app.data.repository

import com.fabricfit.app.domain.model.TrendAffinity
import com.fabricfit.app.domain.model.TrendAnalytics
import com.fabricfit.app.domain.model.TrendPrediction
import com.fabricfit.app.domain.model.TrendingItem
import com.fabricfit.app.domain.model.TrendTimePeriod
import com.fabricfit.app.domain.model.TrendType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.exp
import kotlin.random.Random

/**
 * Repository for trend analytics and predictions.
 */
class TrendAnalyticsRepository {
    
    // In-memory cache (replace with actual database/API in production)
    private var cachedAnalytics: TrendAnalytics? = null
    private var lastFetchTime: Long = 0
    private val cacheDuration = 5 * 60 * 1000L // 5 minutes
    
    /**
     * Get trending items by type and time period.
     */
    suspend fun getTrendingItems(
        type: TrendType,
        timePeriod: TrendTimePeriod = TrendTimePeriod.LAST_7_DAYS,
        limit: Int = 20,
    ): List<TrendingItem> {
        refreshCacheIfNeeded()
        return when (type) {
            TrendType.HASHTAG -> cachedAnalytics?.trendingHashtags ?: emptyList()
            TrendType.STYLE -> cachedAnalytics?.trendingStyles ?: emptyList()
            else -> generateMockTrends(type, timePeriod, limit)
        }.take(limit)
    }
    
    /**
     * Get trend predictions based on historical data.
     */
    suspend fun getTrendPredictions(limit: Int = 10): List<TrendPrediction> {
        refreshCacheIfNeeded()
        return cachedAnalytics?.predictions?.take(limit) ?: generateMockPredictions(limit)
    }
    
    /**
     * Get user's trend affinities.
     */
    suspend fun getUserTrendAffinities(userId: String): List<TrendAffinity> {
        refreshCacheIfNeeded()
        return cachedAnalytics?.userAffinities?.filter { it.userId == userId } ?: emptyList()
    }
    
    /**
     * Record user interaction with a trend.
     */
    suspend fun recordTrendInteraction(userId: String, trendId: String) {
        // In production, this would update the database
        // For now, we'll just log it
        println("Recorded interaction: user=$userId, trend=$trendId")
    }
    
    /**
     * Get personalized trend recommendations for a user.
     */
    suspend fun getPersonalizedTrends(userId: String, limit: Int = 10): List<TrendingItem> {
        val affinities = getUserTrendAffinities(userId)
        val allTrends = getTrendingItems(TrendType.HASHTAG) + getTrendingItems(TrendType.STYLE)
        
        // Sort by affinity score and growth rate
        return allTrends
            .map { trend ->
                val affinity = affinities.find { it.trendId == trend.id }?.affinityScore ?: 0.5f
                trend to (affinity * 0.6f + (trend.growthRate / 100f) * 0.4f)
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
    
    /**
     * Search for trends by name.
     */
    suspend fun searchTrends(query: String, limit: Int = 20): List<TrendingItem> {
        val allTrends = getTrendingItems(TrendType.HASHTAG) + 
                        getTrendingItems(TrendType.STYLE) +
                        getTrendingItems(TrendType.COLOR) +
                        getTrendingItems(TrendType.PATTERN)
        
        return allTrends
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { it.postCount }
            .take(limit)
    }
    
    /**
     * Get "What's Hot" recommendations.
     */
    suspend fun getWhatsHot(limit: Int = 5): List<TrendingItem> {
        val hashtags = getTrendingItems(TrendType.HASHTAG, TrendTimePeriod.LAST_24_HOURS, limit / 2)
        val styles = getTrendingItems(TrendType.STYLE, TrendTimePeriod.LAST_24_HOURS, limit / 2)
        
        return (hashtags + styles)
            .sortedByDescending { it.growthRate }
            .take(limit)
    }
    
    /**
     * Refresh cache if expired.
     */
    private suspend fun refreshCacheIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (cachedAnalytics == null || currentTime - lastFetchTime > cacheDuration) {
            cachedAnalytics = fetchTrendAnalytics()
            lastFetchTime = currentTime
        }
    }
    
    /**
     * Fetch trend analytics from API/database.
     */
    private suspend fun fetchTrendAnalytics(): TrendAnalytics {
        // In production, this would fetch from a real API
        // For now, generate mock data
        return TrendAnalytics(
            trendingHashtags = generateMockTrends(TrendType.HASHTAG, TrendTimePeriod.LAST_7_DAYS, 20),
            trendingStyles = generateMockTrends(TrendType.STYLE, TrendTimePeriod.LAST_7_DAYS, 20),
            predictions = generateMockPredictions(10),
            userAffinities = emptyList(), // Would be fetched per user
            lastUpdated = System.currentTimeMillis(),
        )
    }
    
    /**
     * Generate mock trending items for demo purposes.
     */
    private fun generateMockTrends(
        type: TrendType,
        timePeriod: TrendTimePeriod,
        count: Int,
    ): List<TrendingItem> {
        val baseNames = when (type) {
            TrendType.HASHTAG -> listOf(
                "OOTD", "StreetStyle", "VintageFashion", "SustainableFashion",
                "MinimalistStyle", "BohoChic", "Athleisure", "HighFashion",
                "DIYFashion", "ThriftFlip", "CapsuleWardrobe", "ColorBlocking"
            )
            TrendType.STYLE -> listOf(
                "Minimalist", "Bohemian", "Classic", "Edgy",
                "Romantic", "Sporty", "Preppy", "Grunge"
            )
            TrendType.COLOR -> listOf(
                "Pastel Pink", "Navy Blue", "Forest Green", "Mustard Yellow",
                "Burgundy", "Lavender", "Coral", "Sage Green"
            )
            TrendType.PATTERN -> listOf(
                "Floral", "Stripes", "Polka Dots", "Plaid",
                "Animal Print", "Geometric", "Abstract", "Paisley"
            )
            TrendType.BRAND -> listOf(
                "Nike", "Adidas", "Zara", "H&M",
                "Uniqlo", "Levi's", "Gucci", "Prada"
            )
        }
        
        return baseNames.take(count).mapIndexed { index, name ->
            TrendingItem(
                id = "${type.name}_${name.hashCode()}",
                name = if (type == TrendType.HASHTAG) "#$name" else name,
                type = type,
                postCount = Random.nextInt(100, 10000),
                growthRate = Random.nextFloat() * 100f,
                timePeriod = timePeriod,
                rank = index + 1,
                tags = listOf(type.name.lowercase()),
            )
        }.sortedByDescending { it.postCount }
    }
    
    /**
     * Generate mock trend predictions.
     */
    private fun generateMockPredictions(count: Int): List<TrendPrediction> {
        val trends = generateMockTrends(TrendType.STYLE, TrendTimePeriod.LAST_30_DAYS, count)
        
        return trends.map { trend ->
            TrendPrediction(
                item = trend,
                predictedGrowth = Random.nextFloat() * 200f,
                confidence = Random.nextFloat() * 0.5f + 0.5f, // 0.5 to 1.0
                peakDate = System.currentTimeMillis() + Random.nextLong(7, 30) * 24 * 60 * 60 * 1000,
            )
        }
    }
}
