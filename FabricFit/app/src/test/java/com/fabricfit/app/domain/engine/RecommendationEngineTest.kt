package com.fabricfit.app.domain.engine

import com.fabricfit.app.domain.model.ColorPalette
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.FabricColor
import com.fabricfit.app.domain.model.GarmentSilhouette
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.Measurements
import com.fabricfit.app.domain.model.Occasion
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.domain.model.StyleCategory
import com.fabricfit.app.domain.model.TextureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private val kenteFabric = Fabric(
        id = 1,
        imageUri = "file:///x",
        category = FabricCategory.KENTE,
        confidence = 0.8f,
        pattern = PatternType.STRIPED,
        texture = TextureType.WOVEN,
        motifScale = 0.7f,
        colors = listOf(FabricColor("gold", 0xFFC8952B, 0.5f)),
        palette = ColorPalette(warm = true, saturation = 0.75f, brightness = 0.55f),
        suggestedUses = listOf(Occasion.WEDDING, Occasion.CEREMONY),
        createdAt = 0,
    )

    private val kaba = DressStyle(
        id = "kaba",
        titleKey = "style_kaba_title",
        descriptionKey = "style_kaba_desc",
        gender = GenderCategory.WOMEN,
        category = StyleCategory.TRADITIONAL,
        occasions = setOf(Occasion.WEDDING, Occasion.CEREMONY),
        silhouette = GarmentSilhouette.KABA,
        affinity = setOf(FabricCategory.KENTE),
        structure = 0.75f,
    )

    private val casualShirt = kaba.copy(
        id = "shirt",
        gender = GenderCategory.MEN,
        category = StyleCategory.CASUAL,
        occasions = setOf(Occasion.EVERYDAY),
        silhouette = GarmentSilhouette.PRINT_SHIRT,
        affinity = setOf(FabricCategory.ANKARA),
        structure = 0.3f,
    )

    @Test
    fun `traditional structured Kente ranks Kaba above casual shirt`() {
        val ranked = RecommendationEngine.recommend(kenteFabric, listOf(casualShirt, kaba))
        assertEquals("kaba", ranked.first().style.id)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    @Test
    fun `dismissed styles are filtered out`() {
        val ranked = RecommendationEngine.recommend(kenteFabric, listOf(casualShirt, kaba), dismissed = setOf("kaba"))
        assertEquals(1, ranked.size)
        assertEquals("shirt", ranked.single().style.id)
    }

    @Test
    fun `context-aware FitMatch ranker boosts score with measurements, weave anisotropy and skin harmony`() {
        val baseScore = RecommendationEngine.recommend(kenteFabric, listOf(kaba)).first().score
        val context = RecommendationEngine.RecommendationContext(
            skinToneArgb = 0xFF8D5524L,
            measurements = Measurements(heightCm = 170, waistCm = 68, hipCm = 98),
            weaveAnisotropy = 0.65f,
            favoriteStyleIds = setOf("kaba"),
            preferredGender = GenderCategory.WOMEN,
        )
        val contextScore = RecommendationEngine.recommend(
            fabric = kenteFabric,
            styles = listOf(kaba),
            context = context,
        ).first().score
        assertTrue(
            "expected personalized FitMatch context to boost score ($contextScore > $baseScore)",
            contextScore > baseScore,
        )
    }
}
