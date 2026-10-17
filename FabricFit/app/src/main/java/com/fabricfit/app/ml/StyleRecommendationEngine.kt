package com.fabricfit.app.ml

import com.fabricfit.app.domain.model.ClothingType
import com.fabricfit.app.domain.model.ColorPreference
import com.fabricfit.app.domain.model.CompleteTheLookSuggestion
import com.fabricfit.app.domain.model.OutfitRecommendation
import com.fabricfit.app.domain.model.SimilarUser
import com.fabricfit.app.domain.model.StyleAffinity
import com.fabricfit.app.domain.model.StyleProfile
import com.fabricfit.app.domain.model.StyleWardrobeItem
import kotlin.random.Random

/**
 * ML-powered style recommendation engine.
 * 
 * In production, this would use collaborative filtering, content-based filtering,
 * or hybrid recommendation systems.
 */
class StyleRecommendationEngine {
    
    // Style compatibility matrix (simplified)
    private val styleCompatibility = mapOf(
        "Casual" to listOf("Casual", "Sporty", "Streetwear"),
        "Formal" to listOf("Formal", "Business", "Classic"),
        "Bohemian" to listOf("Bohemian", "Vintage", "Romantic"),
        "Minimalist" to listOf("Minimalist", "Modern", "Classic"),
        "Streetwear" to listOf("Streetwear", "Casual", "Sporty"),
        "Vintage" to listOf("Vintage", "Bohemian", "Classic"),
    )
    
    // Color compatibility (simplified)
    private val colorCompatibility = mapOf(
        "Black" to listOf("White", "Gray", "Red", "Blue", "Pink"),
        "White" to listOf("Black", "Blue", "Navy", "Gray", "Pastel"),
        "Navy" to listOf("White", "Beige", "Gray", "Red"),
        "Gray" to listOf("Black", "White", "Blue", "Pink"),
        "Beige" to listOf("Navy", "Brown", "White", "Green"),
    )
    
    /**
     * Generate outfit recommendations based on user's wardrobe.
     */
    fun generateOutfitRecommendations(
        wardrobe: List<StyleWardrobeItem>,
        occasion: String? = null,
        limit: Int = 5,
    ): List<OutfitRecommendation> {
        if (wardrobe.size < 3) return emptyList()
        
        val recommendations = mutableListOf<OutfitRecommendation>()
        
        // Group items by type
        val tops = wardrobe.filter { it.type in listOf(ClothingType.SHIRT, ClothingType.JACKET) }
        val bottoms = wardrobe.filter { it.type in listOf(ClothingType.PANTS, ClothingType.SKIRT) }
        val shoes = wardrobe.filter { it.type == ClothingType.SHOES }
        val accessories = wardrobe.filter { it.type in listOf(ClothingType.BAG, ClothingType.ACCESSORIES) }
        
        // Generate combinations
        repeat(limit) { index ->
            if (tops.isNotEmpty() && bottoms.isNotEmpty()) {
                val top = tops.random()
                val bottom = bottoms.random()
                val shoe = shoes.randomOrNull()
                val accessory = accessories.randomOrNull()
                
                val items = mutableListOf(top, bottom)
                shoe?.let { items.add(it) }
                accessory?.let { items.add(it) }
                
                val style = determineOutfitStyle(items)
                val confidence = calculateOutfitConfidence(items)
                
                recommendations.add(
                    OutfitRecommendation(
                        id = "outfit_${System.currentTimeMillis()}_$index",
                        name = generateOutfitName(style, occasion),
                        items = items,
                        style = style,
                        occasion = occasion ?: determineOccasion(style),
                        confidenceScore = confidence,
                        reason = generateRecommendationReason(items, style),
                        similarUsers = Random.nextInt(50, 500),
                    )
                )
            }
        }
        
        return recommendations.sortedByDescending { it.confidenceScore }
    }
    
    /**
     * Generate "Complete the Look" suggestions for a specific item.
     */
    fun generateCompleteTheLook(
        currentItem: StyleWardrobeItem,
        wardrobe: List<StyleWardrobeItem>,
        limit: Int = 3,
    ): CompleteTheLookSuggestion {
        val suggestedItems = mutableListOf<StyleWardrobeItem>()
        
        // Find complementary items
        val complementaryTypes = getComplementaryTypes(currentItem.type)
        val candidates = wardrobe.filter { 
            it.type in complementaryTypes && 
            isColorCompatible(currentItem.color, it.color) &&
            isStyleCompatible(currentItem.style, it.style)
        }
        
        // Select top matches
        suggestedItems.addAll(candidates.take(limit))
        
        return CompleteTheLookSuggestion(
            currentItem = currentItem,
            suggestedItems = suggestedItems,
            reason = generateCompleteTheLookReason(currentItem, suggestedItems),
            styleMatch = calculateStyleMatch(currentItem, suggestedItems),
        )
    }
    
    /**
     * Analyze user's wardrobe and generate style profile.
     */
    fun analyzeWardrobe(wardrobe: List<StyleWardrobeItem>, userId: String): StyleProfile {
        // Analyze styles
        val styleCounts = wardrobe.groupingBy { it.style }.eachCount()
        val totalItems = wardrobe.size.toFloat()
        val dominantStyles = styleCounts.map { (style, count) ->
            StyleAffinity(
                style = style,
                percentage = count / totalItems,
                itemCount = count,
            )
        }.sortedByDescending { it.percentage }.take(5)
        
        // Analyze colors
        val colorCounts = wardrobe.groupingBy { it.color }.eachCount()
        val favoriteColors = colorCounts.map { (color, count) ->
            ColorPreference(
                color = color,
                percentage = count / totalItems,
                itemCount = count,
            )
        }.sortedByDescending { it.percentage }.take(5)
        
        // Most worn types
        val typeCounts = wardrobe.groupingBy { it.type }.eachCount()
        val mostWornTypes = typeCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        // Calculate style score (based on variety and balance)
        val styleScore = calculateStyleScore(wardrobe, dominantStyles)
        
        // Generate recommendations
        val recommendations = generateStyleTips(wardrobe, dominantStyles)
        
        return StyleProfile(
            userId = userId,
            dominantStyles = dominantStyles,
            favoriteColors = favoriteColors,
            wardrobeSize = wardrobe.size,
            mostWornTypes = mostWornTypes,
            styleScore = styleScore,
            recommendations = recommendations,
        )
    }
    
    /**
     * Find similar users based on style preferences.
     */
    fun findSimilarUsers(
        userProfile: StyleProfile,
        allProfiles: List<StyleProfile>,
        limit: Int = 5,
    ): List<SimilarUser> {
        return allProfiles
            .filter { it.userId != userProfile.userId }
            .map { profile ->
                val similarity = calculateStyleSimilarity(userProfile, profile)
                val commonStyles = findCommonStyles(userProfile, profile)
                
                SimilarUser(
                    userId = profile.userId,
                    userName = "User ${profile.userId.takeLast(4)}",
                    similarityScore = similarity,
                    commonStyles = commonStyles,
                    commonItems = Random.nextInt(5, 20),
                )
            }
            .sortedByDescending { it.similarityScore }
            .take(limit)
    }
    
    /**
     * Determine outfit style based on items.
     */
    private fun determineOutfitStyle(items: List<StyleWardrobeItem>): String {
        val styles = items.map { it.style }
        return styles.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Casual"
    }
    
    /**
     * Calculate outfit confidence score.
     */
    private fun calculateOutfitConfidence(items: List<StyleWardrobeItem>): Float {
        // Check color compatibility
        val colors = items.map { it.color }
        val colorScore = if (colors.size >= 2) {
            var compatible = 0
            for (i in 0 until colors.size - 1) {
                if (isColorCompatible(colors[i], colors[i + 1])) compatible++
            }
            compatible.toFloat() / (colors.size - 1)
        } else 1.0f
        
        // Check style compatibility
        val styles = items.map { it.style }
        val styleScore = if (styles.size >= 2) {
            var compatible = 0
            for (i in 0 until styles.size - 1) {
                if (isStyleCompatible(styles[i], styles[i + 1])) compatible++
            }
            compatible.toFloat() / (styles.size - 1)
        } else 1.0f
        
        return (colorScore * 0.5f + styleScore * 0.5f)
    }
    
    /**
     * Check if two colors are compatible.
     */
    private fun isColorCompatible(color1: String, color2: String): Boolean {
        val compatible = colorCompatibility[color1] ?: return true
        return color2 in compatible || color1 == color2
    }
    
    /**
     * Check if two styles are compatible.
     */
    private fun isStyleCompatible(style1: String, style2: String): Boolean {
        val compatible = styleCompatibility[style1] ?: return true
        return style2 in compatible || style1 == style2
    }
    
    /**
     * Get complementary clothing types.
     */
    private fun getComplementaryTypes(type: ClothingType): List<ClothingType> {
        return when (type) {
            ClothingType.SHIRT -> listOf(ClothingType.PANTS, ClothingType.SKIRT, ClothingType.SHOES)
            ClothingType.PANTS -> listOf(ClothingType.SHIRT, ClothingType.SHOES, ClothingType.JACKET)
            ClothingType.DRESS -> listOf(ClothingType.SHOES, ClothingType.BAG, ClothingType.ACCESSORIES)
            ClothingType.SHOES -> listOf(ClothingType.PANTS, ClothingType.SKIRT, ClothingType.DRESS)
            else -> ClothingType.values().filter { it != type }
        }
    }
    
    /**
     * Calculate style match score.
     */
    private fun calculateStyleMatch(current: StyleWardrobeItem, suggested: List<StyleWardrobeItem>): Float {
        if (suggested.isEmpty()) return 0f
        
        val styleMatches = suggested.count { isStyleCompatible(current.style, it.style) }
        val colorMatches = suggested.count { isColorCompatible(current.color, it.color) }
        
        return ((styleMatches + colorMatches) / (suggested.size * 2)).toFloat()
    }
    
    /**
     * Calculate style similarity between two profiles.
     */
    private fun calculateStyleSimilarity(profile1: StyleProfile, profile2: StyleProfile): Float {
        val styles1 = profile1.dominantStyles.map { it.style }.toSet()
        val styles2 = profile2.dominantStyles.map { it.style }.toSet()
        
        val intersection = styles1.intersect(styles2).size.toFloat()
        val union = styles1.union(styles2).size.toFloat()
        
        return if (union > 0) intersection / union else 0f
    }
    
    /**
     * Find common styles between profiles.
     */
    private fun findCommonStyles(profile1: StyleProfile, profile2: StyleProfile): List<String> {
        val styles1 = profile1.dominantStyles.map { it.style }
        val styles2 = profile2.dominantStyles.map { it.style }
        return styles1.intersect(styles2).toList()
    }
    
    /**
     * Calculate overall style score.
     */
    private fun calculateStyleScore(wardrobe: List<StyleWardrobeItem>, styles: List<StyleAffinity>): Float {
        // Based on variety, balance, and completeness
        val varietyScore = (styles.size / 5.0f).coerceAtMost(1.0f)
        val balanceScore = if (styles.isNotEmpty()) {
            1.0f - (styles.first().percentage - styles.last().percentage)
        } else 0.5f
        
        return (varietyScore * 0.6f + balanceScore * 0.4f).coerceIn(0f, 1f)
    }
    
    /**
     * Generate style tips based on wardrobe analysis.
     */
    private fun generateStyleTips(wardrobe: List<StyleWardrobeItem>, styles: List<StyleAffinity>): List<String> {
        val tips = mutableListOf<String>()
        
        if (wardrobe.size < 10) {
            tips.add("Build a capsule wardrobe with versatile basics")
        }
        
        if (styles.size == 1) {
            tips.add("Experiment with different styles to expand your fashion range")
        }
        
        val colors = wardrobe.map { it.color }.distinct()
        if (colors.size < 5) {
            tips.add("Add more color variety to your wardrobe")
        }
        
        if (wardrobe.none { it.type == ClothingType.ACCESSORIES }) {
            tips.add("Accessories can elevate any outfit - consider adding some!")
        }
        
        return tips.take(3)
    }
    
    private fun generateOutfitName(style: String, occasion: String?): String {
        val adjectives = listOf("Chic", "Effortless", "Bold", "Classic", "Modern")
        return "${adjectives.random()} $style ${occasion ?: "Look"}"
    }
    
    private fun determineOccasion(style: String): String {
        return when (style) {
            "Formal", "Business" -> "Work"
            "Casual", "Sporty" -> "Everyday"
            "Bohemian", "Romantic" -> "Date Night"
            else -> "Casual Outing"
        }
    }
    
    private fun generateRecommendationReason(items: List<StyleWardrobeItem>, style: String): String {
        return "This $style combination works well together with complementary colors and styles."
    }
    
    private fun generateCompleteTheLookReason(current: StyleWardrobeItem, suggested: List<StyleWardrobeItem>): String {
        return "These items complement your ${current.name.lowercase()} with matching style and colors."
    }
}
