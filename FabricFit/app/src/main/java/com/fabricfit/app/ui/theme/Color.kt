package com.fabricfit.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * FabricFit palette — warm neutrals with madder red, kente gold and deep teal accents.
 * Intentionally NOT dynamic-colour: the brand reads as a fashion product, not a launcher theme.
 */

// Light
val LightPrimary = Color(0xFF8C2F39)
val LightOnPrimary = Color(0xFFFFF8F6)
val LightPrimaryContainer = Color(0xFFFBD9D5)
val LightOnPrimaryContainer = Color(0xFF3B0A10)
val LightSecondary = Color(0xFF7A5A18)
val LightOnSecondary = Color(0xFFFFF9EE)
val LightSecondaryContainer = Color(0xFFF6E1B0)
val LightOnSecondaryContainer = Color(0xFF261A00)
val LightTertiary = Color(0xFF1F5450)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC6E9E2)
val LightOnTertiaryContainer = Color(0xFF00201D)
val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)
val LightBackground = Color(0xFFFFFBF7)
val LightOnBackground = Color(0xFF1D1A17)
val LightSurface = Color(0xFFFFFBF7)
val LightOnSurface = Color(0xFF1D1A17)
val LightSurfaceVariant = Color(0xFFF3E7E0)
val LightOnSurfaceVariant = Color(0xFF524440)
val LightOutline = Color(0xFF85736E)
val LightOutlineVariant = Color(0xFFD8C7C1)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFDF6F1)
val LightSurfaceContainer = Color(0xFFFBF3ED)
val LightSurfaceContainerHigh = Color(0xFFF7F0EA)
val LightSurfaceContainerHighest = Color(0xFFF1EAE4)
val LightInverseSurface = Color(0xFF322F2C)
val LightInverseOnSurface = Color(0xFFF6EFEB)
val LightInversePrimary = Color(0xFFFFB3AB)

// Dark — warm charcoal, not a simple inversion
val DarkPrimary = Color(0xFFFFB3AB)
val DarkOnPrimary = Color(0xFF5A1A20)
val DarkPrimaryContainer = Color(0xFF74252D)
val DarkOnPrimaryContainer = Color(0xFFFFDAD5)
val DarkSecondary = Color(0xFFE4C27E)
val DarkOnSecondary = Color(0xFF402E00)
val DarkSecondaryContainer = Color(0xFF5C4300)
val DarkOnSecondaryContainer = Color(0xFFF6E1B0)
val DarkTertiary = Color(0xFF89CDC4)
val DarkOnTertiary = Color(0xFF003733)
val DarkTertiaryContainer = Color(0xFF00504A)
val DarkOnTertiaryContainer = Color(0xFFC6E9E2)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFF9DEDC)
val DarkBackground = Color(0xFF14110F)
val DarkOnBackground = Color(0xFFECE4E0)
val DarkSurface = Color(0xFF14110F)
val DarkOnSurface = Color(0xFFECE4E0)
val DarkSurfaceVariant = Color(0xFF3A312D)
val DarkOnSurfaceVariant = Color(0xFFD8C7C1)
val DarkOutline = Color(0xFF9F8C87)
val DarkOutlineVariant = Color(0xFF524440)
val DarkSurfaceContainerLowest = Color(0xFF0E0C0A)
val DarkSurfaceContainerLow = Color(0xFF1D1A17)
val DarkSurfaceContainer = Color(0xFF211E1B)
val DarkSurfaceContainerHigh = Color(0xFF272421)
val DarkSurfaceContainerHighest = Color(0xFF322F2C)
val DarkInverseSurface = Color(0xFFECE4E0)
val DarkInverseOnSurface = Color(0xFF322F2C)
val DarkInversePrimary = Color(0xFF8C2F39)

// Brand art accents (theme independent)
val BrandInk = Color(0xFF14110F)
val BrandCream = Color(0xFFFDF8F2)
val BrandGold = Color(0xFFC8952B)
val BrandMadder = Color(0xFF8C2F39)
val BrandTeal = Color(0xFF1F5450)

/**
 * Shared brand brushes — the "woven" gradient family used by hero panels, section ticks and
 * decorative accents. Fixed colours so the brand artwork stays consistent in light and dark.
 */
object BrandGradients {
    /** Deep madder diagonal, top-left to bottom-right; the base wash for hero panels. */
    fun hero(): Brush = Brush.linearGradient(
        colorStops = listOf(0f to Color(0xFF9A3440), 0.5f to Color(0xFF8C2F39), 1f to Color(0xFF5A1E25)),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    /** Vertical gold-to-madder tick used next to section titles. */
    fun tick(): Brush = Brush.linearGradient(
        colorStops = listOf(0f to BrandGold, 1f to BrandMadder),
        start = Offset.Zero,
        end = Offset(0f, 120f),
    )

    /** The three kente-thread colours used by the small woven-dot accent. */
    val weaveDots: List<Color> = listOf(BrandGold, BrandCream, Color(0xFF89CDC4))
}
