package com.ntoma.studio.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class HeritageThemeTest {
    @Test fun userPaletteMatchesExactly() {
        assertEquals(Color(0xFF173F35), LightScheme.primary)
        assertEquals(Color(0xFF65B99A), DarkScheme.primary)
        assertEquals(Color(0xFFD6A84F), LightScheme.secondary)
        assertEquals(Color(0xFFE5B85C), DarkScheme.secondary)
        assertEquals(Color(0xFF7B2937), LightScheme.tertiary)
        assertEquals(Color(0xFFD18A99), DarkScheme.tertiary)
        assertEquals(Color(0xFFF8F3E9), LightScheme.background)
        assertEquals(Color(0xFF0F1F1A), DarkScheme.background)
        assertEquals(Color.White, LightScheme.surface)
        assertEquals(Color(0xFF1B2B25), DarkScheme.surface)
        assertEquals(Color(0xFF252722), LightScheme.onSurface)
        assertEquals(Color(0xFFF2F3F7), DarkScheme.onSurface)
        assertEquals(Color(0xFF706F66), LightScheme.onSurfaceVariant)
        assertEquals(Color(0xFFB7C1BA), DarkScheme.onSurfaceVariant)
        assertEquals(Color(0xFFE2DACA), LightScheme.outlineVariant)
        assertEquals(Color(0xFF34463D), DarkScheme.outlineVariant)
        assertEquals(Color(0xFFEEE7DA), LightScheme.previewCanvas)
    }
    @Test fun functionalTextPairsHaveReadableContrast() {
        for (scheme in listOf(LightScheme, DarkScheme)) {
            for ((foreground, background) in listOf(
                scheme.onPrimary to scheme.primary,
                scheme.onSecondary to scheme.secondary,
                scheme.onSurface to scheme.surface,
                scheme.onSurfaceVariant to scheme.surface,
                scheme.onBackground to scheme.background,
            )) {
                assertTrue("Text contrast must be at least 4.5:1", contrast(foreground, background) >= 4.5)
            }
        }
    }
    private fun contrast(a: Color, b: Color): Double {
        fun lum(c: Color): Double {
            fun linear(v: Float): Double = if (v <= .04045f) v / 12.92 else Math.pow((v + .055) / 1.055, 2.4)
            return .2126 * linear(c.red) + .7152 * linear(c.green) + .0722 * linear(c.blue)
        }
        return (maxOf(lum(a), lum(b)) + .05) / (minOf(lum(a), lum(b)) + .05)
    }
}
