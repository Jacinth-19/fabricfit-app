package com.fabricfit.app.domain.model

/**
 * AI-generated outfit recommendation.
 */
data class OutfitRecommendation(
    val id: String,
    val name: String,
    val items: List<StyleWardrobeItem>,
    val style: String,
    val occasion: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val reason: String,
    val similarUsers: Int, // How many similar users wore this
)

/**
 * Item in user's wardrobe.
 */
data class StyleWardrobeItem(
    val id: String,
    val name: String,
    val type: ClothingType,
    val color: String,
    val pattern: String?,
    val style: String,
    val imageUrl: String,
    val timesWorn: Int = 0,
    val lastWorn: Long? = null,
    val tags: List<String> = emptyList(),
)

/**
 * "Complete the look" suggestion.
 */
data class CompleteTheLookSuggestion(
    val currentItem: StyleWardrobeItem,
    val suggestedItems: List<StyleWardrobeItem>,
    val reason: String,
    val styleMatch: Float, // 0.0 to 1.0
)

/**
 * Style analysis for a user.
 */
data class StyleProfile(
    val userId: String,
    val dominantStyles: List<StyleAffinity>,
    val favoriteColors: List<ColorPreference>,
    val wardrobeSize: Int,
    val mostWornTypes: List<ClothingType>,
    val styleScore: Float, // Overall style rating
    val recommendations: List<String>, // Style tips
)

/**
 * User's preference for a style.
 */
data class StyleAffinity(
    val style: String,
    val percentage: Float, // 0.0 to 1.0
    val itemCount: Int,
)

/**
 * User's preference for a color.
 */
data class ColorPreference(
    val color: String,
    val percentage: Float,
    val itemCount: Int,
)

/**
 * Similar user for style matching.
 */
data class SimilarUser(
    val userId: String,
    val userName: String,
    val similarityScore: Float, // 0.0 to 1.0
    val commonStyles: List<String>,
    val commonItems: Int,
)
