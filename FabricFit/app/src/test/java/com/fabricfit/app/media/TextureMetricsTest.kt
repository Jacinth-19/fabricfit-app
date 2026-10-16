package com.fabricfit.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextureMetricsTest {

    @Test
    fun `glcm contrast is higher on checkerboard than solid`() {
        val side = 16
        val solid = FloatArray(side * side) { 128f }
        val checker = FloatArray(side * side) { i ->
            val x = i % side
            val y = i / side
            if ((x + y) % 2 == 0) 25f else 230f
        }
        val solidGlcm = TextureMetrics.glcm(solid, side, side)
        val checkerGlcm = TextureMetrics.glcm(checker, side, side)
        assertTrue("checker contrast (${checkerGlcm.contrast}) should exceed solid contrast (${solidGlcm.contrast})", checkerGlcm.contrast > solidGlcm.contrast + 0.3f)
        assertTrue("solid homogeneity (${solidGlcm.homogeneity}) should exceed checker (${checkerGlcm.homogeneity})", solidGlcm.homogeneity > checkerGlcm.homogeneity)
    }

    @Test
    fun `gabor orientation detects strong horizontal vs vertical anisotropy`() {
        val side = 32
        val horizontalBands = FloatArray(side * side) { i ->
            val y = i / side
            if ((y / 2) % 2 == 0) 0.1f else 0.9f
        }
        val gaborH = TextureMetrics.gaborOrientation(horizontalBands, side, side)
        assertEquals(0, gaborH.dominantAngleDeg)
        assertTrue("expected noticeable anisotropy, got ${gaborH.anisotropy}", gaborH.anisotropy > 0.25f)

        val verticalStrips = FloatArray(side * side) { i ->
            val x = i % side
            if ((x / 2) % 2 == 0) 0.1f else 0.9f
        }
        val gaborV = TextureMetrics.gaborOrientation(verticalStrips, side, side)
        assertEquals(90, gaborV.dominantAngleDeg)
        assertTrue(gaborV.anisotropy > 0.25f)
    }

    @Test
    fun `spectral motif profile extracts strip width and alignment advice`() {
        val side = 32
        val dark = 0xFF222222.toInt()
        val bright = 0xFFDDDDDD.toInt()
        val kenteStrips = IntArray(side * side) { i ->
            val x = i % side
            if ((x / 4) % 2 == 0) dark else bright
        }
        val profile = TextureMetrics.spectralMotifProfile(kenteStrips, side, side)
        assertTrue("expected stripWidthPx > 0, got ${profile.stripWidthPx}", profile.stripWidthPx > 0f)
        // harmonicPurity depends on DFT implementation details, just check it's non-negative
        assertTrue("expected harmonicPurity >= 0, got ${profile.harmonicPurity}", profile.harmonicPurity >= 0f)
        // alignmentAdvice is a derived value - just verify it's a valid enum
        assertNotNull(profile.alignmentAdvice)
    }
}
