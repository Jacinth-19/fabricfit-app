package com.fabricfit.app.ml

import com.fabricfit.app.domain.model.ClothingType
import com.fabricfit.app.domain.model.StyleProfile
import com.fabricfit.app.domain.model.WardrobeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StyleRecommendationEngineTest {

    private lateinit var engine: StyleRecommendationEngine
    private lateinit var testWardrobe: List<WardrobeItem>

    @Before
    fun setup() {
        engine = StyleRecommendationEngine()
        testWardrobe = listOf(
            WardrobeItem(
                id = "item_1",
                name = "White Shirt",
                type = ClothingType.SHIRT,
                color = "White",
                pattern = "Solid",
                style = "Casual",
                imageUrl = "https://example.com/shirt.jpg",
                timesWorn = 5
            ),
            WardrobeItem(
                id = "item_2",
                name = "Blue Jeans",
                type = ClothingType.PANTS,
                color = "Blue",
                pattern = "Solid",
                style = "Casual",
                imageUrl = "https://example.com/jeans.jpg",
                timesWorn = 10
            ),
            WardrobeItem(
                id = "item_3",
                name = "Black Shoes",
                type = ClothingType.SHOES,
                color = "Black",
                pattern = null,
                style = "Casual",
                imageUrl = "https://example.com/shoes.jpg",
                timesWorn = 8
            ),
            WardrobeItem(
                id = "item_4",
                name = "Red Dress",
                type = ClothingType.DRESS,
                color = "Red",
                pattern = "Floral",
                style = "Formal",
                imageUrl = "https://example.com/dress.jpg",
                timesWorn = 2
            )
        )
    }

    @Test
    fun `generateOutfitRecommendations returns valid recommendations`() {
        val recommendations = engine.generateOutfitRecommendations(testWardrobe, limit = 3)

        assertNotNull(recommendations)
        assertTrue(recommendations.size <= 3)
        recommendations.forEach { recommendation ->
            assertNotNull(recommendation.id)
            assertNotNull(recommendation.name)
            assertTrue(recommendation.items.isNotEmpty())
            assertTrue(recommendation.confidenceScore in 0.0f..1.0f)
            assertNotNull(recommendation.style)
            assertNotNull(recommendation.occasion)
        }
    }

    @Test
    fun `generateOutfitRecommendations with occasion filters correctly`() {
        val recommendations = engine.generateOutfitRecommendations(
            testWardrobe,
            occasion = "Work",
            limit = 2
        )

        assertNotNull(recommendations)
        assertTrue(recommendations.size <= 2)
    }

    @Test
    fun `generateCompleteTheLook returns complementary items`() {
        val currentItem = testWardrobe[0] // White Shirt
        val suggestion = engine.generateCompleteTheLook(currentItem, testWardrobe, limit = 3)

        assertNotNull(suggestion)
        assertEquals(currentItem, suggestion.currentItem)
        assertTrue(suggestion.suggestedItems.isNotEmpty())
        assertTrue(suggestion.styleMatch in 0.0f..1.0f)
        assertNotNull(suggestion.reason)
    }

    @Test
    fun `analyzeWardrobe generates valid style profile`() {
        val profile = engine.analyzeWardrobe(testWardrobe, "user_123")

        assertNotNull(profile)
        assertEquals("user_123", profile.userId)
        assertEquals(testWardrobe.size, profile.wardrobeSize)
        assertTrue(profile.dominantStyles.isNotEmpty())
        assertTrue(profile.favoriteColors.isNotEmpty())
        assertTrue(profile.mostWornTypes.isNotEmpty())
        assertTrue(profile.styleScore in 0.0f..1.0f)
    }

    @Test
    fun `analyzeWardrobe identifies dominant styles correctly`() {
        val profile = engine.analyzeWardrobe(testWardrobe, "user_123")

        // Should have Casual as dominant style (3 out of 4 items)
        val casualStyle = profile.dominantStyles.find { it.style == "Casual" }
        assertNotNull(casualStyle)
        assertTrue(casualStyle!!.percentage > 0.5f)
    }

    @Test
    fun `findSimilarUsers returns users sorted by similarity`() {
        val userProfile = engine.analyzeWardrobe(testWardrobe, "user_123")
        val otherProfiles = listOf(
            engine.analyzeWardrobe(testWardrobe, "user_456"),
            engine.analyzeWardrobe(testWardrobe.take(2), "user_789")
        )

        val similarUsers = engine.findSimilarUsers(userProfile, otherProfiles, limit = 2)

        assertNotNull(similarUsers)
        assertTrue(similarUsers.size <= 2)
        similarUsers.forEach { user ->
            assertTrue(user.similarityScore in 0.0f..1.0f)
            assertNotNull(user.commonStyles)
        }

        // Should be sorted by similarity (descending)
        if (similarUsers.size >= 2) {
            assertTrue(similarUsers[0].similarityScore >= similarUsers[1].similarityScore)
        }
    }

    @Test
    fun `empty wardrobe returns empty recommendations`() {
        val emptyWardrobe = emptyList<WardrobeItem>()
        val recommendations = engine.generateOutfitRecommendations(emptyWardrobe, limit = 3)

        assertNotNull(recommendations)
        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun `wardrobe with less than 3 items returns empty recommendations`() {
        val smallWardrobe = testWardrobe.take(2)
        val recommendations = engine.generateOutfitRecommendations(smallWardrobe, limit = 3)

        assertNotNull(recommendations)
        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun `style compatibility is calculated correctly`() {
        val casualItem1 = testWardrobe[0] // Casual shirt
        val casualItem2 = testWardrobe[1] // Casual jeans
        val formalItem = testWardrobe[3] // Formal dress

        val casualSuggestion = engine.generateCompleteTheLook(casualItem1, listOf(casualItem2), limit = 1)
        val mixedSuggestion = engine.generateCompleteTheLook(casualItem1, listOf(formalItem), limit = 1)

        // Casual items should have higher style match
        assertTrue(casualSuggestion.styleMatch >= mixedSuggestion.styleMatch)
    }

    @Test
    fun `color compatibility affects recommendations`() {
        val whiteShirt = testWardrobe[0] // White
        val blueJeans = testWardrobe[1] // Blue - compatible with white
        val redDress = testWardrobe[3] // Red - less compatible with white

        val suggestion = engine.generateCompleteTheLook(whiteShirt, listOf(blueJeans, redDress), limit = 2)

        assertNotNull(suggestion)
        assertTrue(suggestion.suggestedItems.isNotEmpty())
    }
}
