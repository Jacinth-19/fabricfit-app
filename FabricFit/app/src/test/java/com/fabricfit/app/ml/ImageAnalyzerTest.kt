package com.fabricfit.app.ml

import android.content.Context
import android.graphics.Bitmap
import com.fabricfit.app.domain.model.AttributeType
import com.fabricfit.app.domain.model.ClothingType
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ImageAnalyzer] is a deterministic demo analyser, so these tests pin the
 * contract it promises (shape of the result, value ranges, hashtag rules).
 * A real bitmap is used via Robolectric rather than a mock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageAnalyzerTest {

    private lateinit var analyzer: ImageAnalyzer
    private lateinit var testBitmap: Bitmap

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        analyzer = ImageAnalyzer(context)
        testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun `analyzeImage returns valid analysis result`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        assertNotNull(result)
        assertTrue(result.detectedItems.isNotEmpty())
        assertTrue(result.dominantColors.isNotEmpty())
        assertTrue(result.suggestedHashtags.isNotEmpty())
        assertTrue(result.confidence in 0.0f..1.0f)
    }

    @Test
    fun `analyzeImage detects multiple clothing items`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        // Should detect 2-4 items
        assertTrue(result.detectedItems.size in 2..4)
        result.detectedItems.forEach { item ->
            assertNotNull(item.type)
            assertTrue(item.confidence in 0.0f..1.0f)
        }
    }

    @Test
    fun `analyzeImage includes attributes for detected items`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        result.detectedItems.forEach { item ->
            assertTrue(item.attributes.isNotEmpty())
            item.attributes.forEach { attr ->
                assertNotNull(attr.type)
                assertNotNull(attr.value)
                assertTrue(attr.confidence in 0.0f..1.0f)
            }
        }
    }

    @Test
    fun `analyzeImage detects color attributes`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        val colorAttributes = result.detectedItems.flatMap { item ->
            item.attributes.filter { it.type == AttributeType.COLOR }
        }

        assertTrue(colorAttributes.isNotEmpty())
        val validColors = listOf("Red", "Blue", "Black", "White", "Green", "Yellow", "Pink", "Navy")
        colorAttributes.forEach { attr ->
            assertTrue(attr.value in validColors)
        }
    }

    @Test
    fun `analyzeImage detects pattern attributes`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        val patternAttributes = result.detectedItems.flatMap { item ->
            item.attributes.filter { it.type == AttributeType.PATTERN }
        }

        // Patterns are optional (50% chance), so may or may not be present
        patternAttributes.forEach { attr ->
            val validPatterns = listOf("Striped", "Floral", "Solid", "Plaid", "Polka Dot", "Geometric")
            assertTrue(attr.value in validPatterns)
        }
    }

    @Test
    fun `analyzeImage extracts dominant colors`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        assertTrue(result.dominantColors.isNotEmpty())
        result.dominantColors.forEach { color ->
            assertNotNull(color.name)
            assertNotNull(color.hexCode)
            assertTrue(color.hexCode.startsWith("#"))
            assertTrue(color.percentage in 0.0f..1.0f)
        }

        // Total percentage should be close to 1.0
        val totalPercentage = result.dominantColors.sumOf { it.percentage.toDouble() }
        assertTrue(totalPercentage in 0.9..1.1)
    }

    @Test
    fun `generateHashtags creates relevant hashtags`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)
        val hashtags = analyzer.generateHashtags(result)

        assertTrue(hashtags.isNotEmpty())
        assertTrue(hashtags.size <= 10)
        hashtags.forEach { hashtag ->
            assertTrue(hashtag.startsWith("#"))
        }

        // Should include general fashion hashtags
        assertTrue(hashtags.any { it.contains("fashion", ignoreCase = true) })
        assertTrue(hashtags.any { it.contains("style", ignoreCase = true) })
    }

    @Test
    fun `generateHashtags includes item-specific hashtags`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)
        val hashtags = analyzer.generateHashtags(result)

        // Items contribute their own hashtags; the list is capped at ten
        // entries, so at least one item hashtag is guaranteed to survive.
        val itemHashtags = result.detectedItems.map { "#${it.type.name.lowercase()}" }
        assertTrue(hashtags.any { it in itemHashtags })
    }

    @Test
    fun `analyzeImage determines overall style`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        // Overall style may be null if no style attributes detected
        if (result.overallStyle != null) {
            val validStyles = listOf("Casual", "Formal", "Vintage", "Modern", "Bohemian", "Minimalist")
            assertTrue(result.overallStyle in validStyles)
        }
    }

    @Test
    fun `detected items have bounding boxes`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)

        result.detectedItems.forEach { item ->
            assertNotNull(item.boundingBox)
            item.boundingBox?.let { box ->
                assertTrue(box.left in 0.0f..1.0f)
                assertTrue(box.top in 0.0f..1.0f)
                assertTrue(box.right in 0.0f..1.0f)
                assertTrue(box.bottom in 0.0f..1.0f)
                assertTrue(box.right > box.left)
                assertTrue(box.bottom > box.top)
            }
        }
    }

    @Test
    fun `analyzeImage returns valid clothing types`() = runBlocking {
        val result = analyzer.analyzeImage(testBitmap)
        val validTypes = ClothingType.values().filter { it != ClothingType.UNKNOWN }

        result.detectedItems.forEach { item ->
            assertTrue(item.type in validTypes)
        }
    }

    @Test
    fun `detectColors returns valid color information`() = runBlocking {
        val colors = analyzer.detectColors(testBitmap)

        assertTrue(colors.isNotEmpty())
        colors.forEach { color ->
            assertNotNull(color.name)
            assertNotNull(color.hexCode)
            assertTrue(color.hexCode.matches(Regex("^#[0-9A-F]{6}$")))
            assertTrue(color.percentage in 0.0f..1.0f)
        }
    }

    @Test
    fun `release does not throw exception`() {
        analyzer.release()
        // Should complete without exception
    }
}
