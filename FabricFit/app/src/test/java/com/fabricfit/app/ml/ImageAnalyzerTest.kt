package com.fabricfit.app.ml

import android.content.Context
import android.graphics.Bitmap
import com.fabricfit.app.domain.model.AttributeType
import com.fabricfit.app.domain.model.ClothingType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImageAnalyzerTest {

    private lateinit var analyzer: ImageAnalyzer
    private lateinit var mockContext: Context
    private lateinit var mockBitmap: Bitmap

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockBitmap = mockk(relaxed = true)
        every { mockBitmap.width } returns 100
        every { mockBitmap.height } returns 100
        analyzer = ImageAnalyzer(mockContext)
    }

    @Test
    fun `analyzeImage returns valid analysis result`() = runBlocking {
        val result = analyzer.analyzeImage(mockBitmap)

        assertNotNull(result)
        assertTrue(result.detectedItems.isNotEmpty())
        assertTrue(result.dominantColors.isNotEmpty())
        assertTrue(result.suggestedHashtags.isNotEmpty())
        assertTrue(result.confidence in 0.0f..1.0f)
    }

    @Test
    fun `analyzeImage detects multiple clothing items`() = runBlocking {
        val result = analyzer.analyzeImage(mockBitmap)

        // Should detect 2-4 items
        assertTrue(result.detectedItems.size in 2..4)
        result.detectedItems.forEach { item ->
            assertNotNull(item.type)
            assertTrue(item.confidence in 0.0f..1.0f)
        }
    }

    @Test
    fun `analyzeImage includes attributes for detected items`() = runBlocking {
        val result = analyzer.analyzeImage(mockBitmap)

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
        val result = analyzer.analyzeImage(mockBitmap)

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
        val result = analyzer.analyzeImage(mockBitmap)

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
        val result = analyzer.analyzeImage(mockBitmap)

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
        val result = analyzer.analyzeImage(mockBitmap)
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
        val result = analyzer.analyzeImage(mockBitmap)
        val hashtags = analyzer.generateHashtags(result)

        // Should include hashtags based on detected items
        result.detectedItems.forEach { item ->
            val expectedHashtag = "#${item.type.name.lowercase()}"
            assertTrue(hashtags.contains(expectedHashtag))
        }
    }

    @Test
    fun `analyzeImage determines overall style`() = runBlocking {
        val result = analyzer.analyzeImage(mockBitmap)

        // Overall style may be null if no style attributes detected
        if (result.overallStyle != null) {
            val validStyles = listOf("Casual", "Formal", "Vintage", "Modern", "Bohemian", "Minimalist")
            assertTrue(result.overallStyle in validStyles)
        }
    }

    @Test
    fun `detected items have bounding boxes`() = runBlocking {
        val result = analyzer.analyzeImage(mockBitmap)

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
        val result = analyzer.analyzeImage(mockBitmap)
        val validTypes = ClothingType.values().filter { it != ClothingType.UNKNOWN }

        result.detectedItems.forEach { item ->
            assertTrue(item.type in validTypes)
        }
    }

    @Test
    fun `detectColors returns valid color information`() = runBlocking {
        val colors = analyzer.detectColors(mockBitmap)

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
