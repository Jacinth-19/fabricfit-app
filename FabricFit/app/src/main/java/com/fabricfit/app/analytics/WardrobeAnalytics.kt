package com.fabricfit.app.analytics

import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.PatternType
import java.util.Calendar

/**
 * Wardrobe analytics engine for insights and recommendations.
 */
object WardrobeAnalytics {
    
    /**
     * Analyze color distribution in wardrobe.
     * Returns map of color names to count.
     */
    fun analyzeColorDistribution(fabrics: List<Fabric>): Map<String, Int> {
        val colorCounts = mutableMapOf<String, Int>()
        
        fabrics.forEach { fabric ->
            fabric.colors.forEach { color ->
                val colorName = color.colorName.name
                colorCounts[colorName] = (colorCounts[colorName] ?: 0) + 1
            }
        }
        
        return colorCounts.toMap()
    }
    
    /**
     * Analyze fabric category distribution.
     * Returns map of categories to count.
     */
    fun analyzeCategoryDistribution(fabrics: List<Fabric>): Map<FabricCategory, Int> {
        return fabrics.groupBy { it.category }
            .mapValues { it.value.size }
    }
    
    /**
     * Analyze pattern distribution.
     * Returns map of patterns to count.
     */
    fun analyzePatternDistribution(fabrics: List<Fabric>): Map<PatternType, Int> {
        return fabrics.groupBy { it.pattern }
            .mapValues { it.value.size }
    }
    
    /**
     * Get most used fabrics (based on favorites count).
     */
    fun getMostUsedFabrics(fabrics: List<Fabric>, limit: Int = 5): List<Fabric> {
        return fabrics
            .filter { it.isFavorite }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
    
    /**
     * Get least used fabrics (not favorited, oldest first).
     */
    fun getLeastUsedFabrics(fabrics: List<Fabric>, limit: Int = 5): List<Fabric> {
        return fabrics
            .filter { !it.isFavorite }
            .sortedBy { it.createdAt }
            .take(limit)
    }
    
    /**
     * Identify wardrobe gaps - categories with few items.
     */
    fun identifyWardrobeGaps(fabrics: List<Fabric>): List<String> {
        val categoryCounts = analyzeCategoryDistribution(fabrics)
        val gaps = mutableListOf<String>()
        
        // Define expected minimum counts for each category
        val expectedMinimums = mapOf(
            FabricCategory.KENTE to 3,
            FabricCategory.ANKARA to 3,
            FabricCategory.LACE to 2,
            FabricCategory.CHIFFON to 2,
            FabricCategory.SILK to 1,
            FabricCategory.COTTON_PLAIN to 2
        )
        
        expectedMinimums.forEach { (category, minimum) ->
            val current = categoryCounts[category] ?: 0
            if (current < minimum) {
                gaps.add("${category.name} (have $current, need $minimum)")
            }
        }
        
        return gaps
    }
    
    /**
     * Get fabrics not worn in the last X days.
     */
    fun getFabricsNotWornRecently(
        fabrics: List<Fabric>,
        days: Int = 30
    ): List<Fabric> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val cutoffDate = calendar.timeInMillis
        
        return fabrics
            .filter { it.createdAt < cutoffDate }
            .sortedBy { it.createdAt }
    }
    
    /**
     * Get seasonal recommendations based on current month.
     */
    fun getSeasonalRecommendations(fabrics: List<Fabric>): List<String> {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        
        return when (currentMonth) {
            // December, January, February - Harmattan/Cool season
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> {
                listOf(
                    "Try richer fabrics like Kente for the cool season",
                    "Layer with lace or chiffon for elegance",
                    "Deep colors work well in harmattan weather"
                )
            }
            // March, April, May - Hot season
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> {
                listOf(
                    "Light cotton and Ankara are perfect for hot days",
                    "Bright colors reflect the sunny weather",
                    "Loose, flowing styles keep you cool"
                )
            }
            // June, July, August - Rainy season
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> {
                listOf(
                    "Darker fabrics hide rain spots better",
                    "Quick-dry fabrics are practical",
                    "Consider indoor events with formal attire"
                )
            }
            // September, October, November - Wedding season
            Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> {
                listOf(
                    "Wedding season! Time for your finest Kente",
                    "Formal occasion wear is in high demand",
                    "Coordinate with wedding color schemes"
                )
            }
            else -> emptyList()
        }
    }
    
    /**
     * Get color harmony suggestions.
     * Returns colors that would complement the wardrobe.
     */
    fun getColorHarmonySuggestions(fabrics: List<Fabric>): List<String> {
        val colorDist = analyzeColorDistribution(fabrics)
        val suggestions = mutableListOf<String>()
        
        // Check for missing warm colors
        val warmColors = listOf("GOLD", "ORANGE", "RED", "YELLOW")
        val hasWarmColors = warmColors.any { colorDist.containsKey(it) }
        if (!hasWarmColors) {
            suggestions.add("Add warm colors like gold, orange, or red for variety")
        }
        
        // Check for missing cool colors
        val coolColors = listOf("BLUE", "GREEN", "PURPLE")
        val hasCoolColors = coolColors.any { colorDist.containsKey(it) }
        if (!hasCoolColors) {
            suggestions.add("Add cool colors like blue, green, or purple for balance")
        }
        
        // Check for neutrals
        val neutrals = listOf("BLACK", "WHITE", "BROWN", "GRAY")
        val hasNeutrals = neutrals.any { colorDist.containsKey(it) }
        if (!hasNeutrals) {
            suggestions.add("Add neutral colors like black, white, or brown for versatility")
        }
        
        return suggestions
    }
    
    /**
     * Generate wardrobe summary statistics.
     */
    fun generateWardrobeSummary(fabrics: List<Fabric>): WardrobeSummary {
        return WardrobeSummary(
            totalFabrics = fabrics.size,
            favoriteCount = fabrics.count { it.isFavorite },
            categoryDistribution = analyzeCategoryDistribution(fabrics),
            patternDistribution = analyzePatternDistribution(fabrics),
            colorDistribution = analyzeColorDistribution(fabrics),
            oldestFabricDate = fabrics.minByOrNull { it.createdAt }?.createdAt,
            newestFabricDate = fabrics.maxByOrNull { it.createdAt }?.createdAt,
            gaps = identifyWardrobeGaps(fabrics),
            colorSuggestions = getColorHarmonySuggestions(fabrics)
        )
    }
}

/**
 * Data class for wardrobe summary.
 */
data class WardrobeSummary(
    val totalFabrics: Int,
    val favoriteCount: Int,
    val categoryDistribution: Map<FabricCategory, Int>,
    val patternDistribution: Map<PatternType, Int>,
    val colorDistribution: Map<String, Int>,
    val oldestFabricDate: Long?,
    val newestFabricDate: Long?,
    val gaps: List<String>,
    val colorSuggestions: List<String>
)
