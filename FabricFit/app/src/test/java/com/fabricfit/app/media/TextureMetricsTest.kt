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
        val solidGlcm = TextureMetrics.glcm(solid, side)
        val checkerGlcm = TextureMetrics.glcm(checker, side)
        assertTrue(checkerGlcm.contrast > solidGlcm.contrast + 0.3f)
        assertTrue(solidGlcm.homogeneity > checkerGlcm.homogeneity)
    }

    @Test
    fun `periodicity detects regular horizontal stripes`() {
        val side = 32
        val period = 4
        val striped = FloatArray(side * side) { i ->
            val y = i / side
            if ((y / (period / 2)) % 2 == 0) 0.1f else 0.9f
        }
        val (_, py) = TextureMetrics.periodicity(striped, side)
        assertTrue("expected vertical periodicity close to $period, got $py", py in 3..6)
    }

    @Test
    fun `lbp entropy is low on uniform region and higher on varied texture`() {
        val side = 16
        val flat = FloatArray(side * side) { 0.4f }
        val ramp = FloatArray(side * side) { i -> ((i * 37) % 100) / 100f }
        assertTrue(TextureMetrics.lbpEntropy(ramp, side) > TextureMetrics.lbpEntropy(flat, side))
    }

    @Test
    fun `gabor orientation detects strong horizontal vs vertical anisotropy`() {
        val side = 32
        val horizontalBands = FloatArray(side * side) { i ->
            val y = i / side
            if ((y / 2) % 2 == 0) 0.1f else 0.9f
        }
        val gaborH = TextureMetrics.gaborOrientation(horizontalBands, side)
        assertEquals(0, gaborH.dominantAngleDeg)
        assertTrue("expected noticeable anisotropy, got ${gaborH.anisotropy}", gaborH.anisotropy > 0.25f)

        val verticalStrips = FloatArray(side * side) { i ->
            val x = i % side
            if ((x / 2) % 2 == 0) 0.1f else 0.9f
        }
        val gaborV = TextureMetrics.gaborOrientation(verticalStrips, side)
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
        val profile = TextureMetrics.spectralMotifProfile(kenteStrips, side)
        assertTrue(profile.stripWidthNormalized > 0f)
        assertTrue(profile.harmonicPurity > 0.2f)
        assertEquals(TextureMetrics.AlignmentAdvice.VERTICAL_STRIP_ALIGN, profile.alignmentAdvice)
    }
}
