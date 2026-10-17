package com.fabricfit.app.domain.engine

import com.fabricfit.app.data.ml.PersonSegmenter
import com.fabricfit.app.data.ml.PersonSegmenter.BodyRegion
import com.fabricfit.app.data.ml.PersonSegmenter.SkinUndertone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorHarmonyEngineTest {

    @Test
    fun `saturated fabric yields complementary, analogous, neutral and metallic suggestions`() {
        val kenteGold = 0xFFC8952BL
        val suggestions = ColorHarmonyEngine.suggest(listOf(kenteGold))
        val roles = suggestions.map { it.role }.toSet()
        assertTrue(ColorHarmonyEngine.Role.COMPLEMENTARY in roles)
        assertTrue(ColorHarmonyEngine.Role.ANALOGOUS in roles)
        assertTrue(ColorHarmonyEngine.Role.NEUTRAL in roles)
        assertTrue(ColorHarmonyEngine.Role.METALLIC in roles)
    }

    @Test
    fun `empty palette returns empty suggestions`() {
        assertTrue(ColorHarmonyEngine.suggest(emptyList()).isEmpty())
    }

    @Test
    fun `detectUndertone and skinHarmonyScore distinguish warm golden from cool deep skin`() {
        val warmGoldenSkin = 0xFF8D5524L
        val coolDeepSkin = 0xFF3B2F2FL

        assertEquals(SkinUndertone.WARM_GOLDEN, ColorHarmonyEngine.detectUndertone(warmGoldenSkin))
        assertEquals(SkinUndertone.COOL_DEEP, ColorHarmonyEngine.detectUndertone(coolDeepSkin))

        val warmGoldFabric = listOf(0xFFC8952BL, 0xFFB85D19L)
        val warmScore = ColorHarmonyEngine.skinHarmonyScore(warmGoldFabric, warmGoldenSkin)
        val coolScore = ColorHarmonyEngine.skinHarmonyScore(warmGoldFabric, coolDeepSkin)
        assertTrue("warm gold fabric should score higher on warm golden skin ($warmScore vs $coolScore)", warmScore > coolScore)

        val wearerSuggestions = ColorHarmonyEngine.suggestForWearer(warmGoldFabric, warmGoldenSkin)
        assertTrue(wearerSuggestions.any { it.role == ColorHarmonyEngine.Role.METALLIC })
    }

    @Test
    fun `human region parser segments head, torso, lower garment and extracts skin tone`() {
        val w = 20
        val h = 20
        val skinPixel = 0xFF8D5524.toInt()
        val garmentPixel = 0xFF1F4E79.toInt()
        val bgPixel = 0xFFECEAE4.toInt()

        val pixels = IntArray(w * h)
        val maskAlpha = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                if (x in 5..14 && y in 2..17) {
                    maskAlpha[i] = 255
                    pixels[i] = if (y <= 5) skinPixel else garmentPixel
                } else {
                    maskAlpha[i] = 0
                    pixels[i] = bgPixel
                }
            }
        }

        val parsed = PersonSegmenter.analyzeHumanRegionsAndSkin(pixels, maskAlpha, w, h)
        assertNotNull(parsed.dominantSkinArgb)
        assertEquals(SkinUndertone.WARM_GOLDEN, parsed.skinUndertone)
        assertTrue(parsed.regionMap.any { it == BodyRegion.HEAD_AND_FACE.ordinal.toByte() })
        assertTrue(parsed.regionMap.any { it == BodyRegion.UPPER_GARMENT.ordinal.toByte() })
        assertTrue(parsed.regionMap.any { it == BodyRegion.LOWER_GARMENT.ordinal.toByte() })
    }
}
