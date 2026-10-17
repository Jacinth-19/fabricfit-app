package com.fabricfit.app.domain.model

/**
 * Represents a trending hashtag or style.
 */
data class TrendingItem(
    val id: String,
    val name: String,
    val type: TrendType,
    val postCount: Int,
    val growthRate: Float, // Percentage growth over time period
    val timePeriod: TrendTimePeriod,
    val rank: Int,
    val tags: List<String> = emptyList(),
)

enum class TrendType {
    HASHTAG,
    STYLE,
    COLOR,
    PATTERN,
    BRAND,
}

enum class TrendTimePeriod {
    LAST_24_HOURS,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
}

/**
 * Trend prediction based on historical data.
 */
data class TrendPrediction(
    val item: TrendingItem,
    val predictedGrowth: Float,
    val confidence: Float, // 0.0 to 1.0
    val peakDate: Long? = null, // Timestamp when trend is expected to peak
)

/**
 * User's trend affinity score.
 */
data class TrendAffinity(
    val userId: String,
    val trendId: String,
    val affinityScore: Float, // 0.0 to 1.0
    val interactionCount: Int,
    val lastInteraction: Long,
)

/**
 * Trend analytics summary.
 */
data class TrendAnalytics(
    val trendingHashtags: List<TrendingItem>,
    val trendingStyles: List<TrendingItem>,
    val predictions: List<TrendPrediction>,
    val userAffinities: List<TrendAffinity>,
    val lastUpdated: Long,
)
