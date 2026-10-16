package com.fabricfit.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextureMetricsTest {

    @Test
    fun `glcm contrast is higher on checkerboard than solid`() {
        val side = 16
        val solid = FloatArray(side * side) { 0.5f }
        val checker = FloatArray(side * side) { i ->
            val x = i % side
            val y = i / side
            if ((x + y) % 2 == 0) 0.1f else 0.9f
        }
        val solidGlcm = TextureMetrics.glcm(solid, side, side)
        val checkerGlcm = TextureMetrics.glcm(checker, side, side)
        assertTrue(checkerGlcm.contrast > solidGlcm.contrast + 0.3f)
        assertTrue(solidGlcm.homogeneity > checkerGlcm.homogeneity)
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
        val kenteStrips = FloatArray(side * side) { i ->
            val x = i % side
            if ((x / 3) % 2 == 0) 0.15f else 0.85f
        }
        val profile = TextureMetrics.spectralMotifProfile(kenteStrips, side, side)
        assertTrue(profile.stripWidthPx > 0f)
        assertTrue(profile.harmonicPurity > 0.2f)
        assertEquals(TextureMetrics.AlignmentAdvice.MATCH_WARP_STRIPS, profile.alignmentAdvice)
    }
}
