package com.ntoma.studio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Heritage Luxe design system (see docs/heritage_luxe_design.md + the brand board).
 * Forest green carries primary actions, kente gold the highlights/active states,
 * burgundy the accents, warm ivory the light surfaces. Intentionally NOT dynamic-colour:
 * the brand reads as a fashion product, not a launcher theme.
 */

// Brand tokens (theme independent)
val ForestGreen = Color(0xFF173F35)
val KenteGold = Color(0xFFD6A84F)
val Burgundy = Color(0xFF7B2937)
val WarmIvory = Color(0xFFF8F3E9)
val Charcoal = Color(0xFF252722)
val StoneGrey = Color(0xFF706F66)
val LightGrey = Color(0xFFD9D6CC)
val SuccessGreen = Color(0xFF2E7D32)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFD32F2F)
val InfoBlue = Color(0xFF3882F6)

// Legacy art accents kept for procedural swatches/illustrations — re-tuned to the brand.
val BrandInk = Charcoal
val BrandCream = WarmIvory
val BrandGold = KenteGold
val BrandMadder = Burgundy
val BrandTeal = ForestGreen

// Light theme: background #F8F3E9 · surface #FFFFFF · card #FFFFFF · text #252722 / #706F66
val LightPrimary = ForestGreen
val LightOnPrimary = Color(0xFFFBF7EE)
val LightPrimaryContainer = Color(0xFFDDEAE2)
val LightOnPrimaryContainer = Color(0xFF072A21)
val LightSecondary = Color(0xFF7A5A18) // accessible gold-ink for text-on-light
val LightOnSecondary = Color(0xFFFFF9EE)
val LightSecondaryContainer = Color(0xFFF6E4BE)
val LightOnSecondaryContainer = Color(0xFF261A00)
val LightTertiary = Burgundy
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFBD9DC)
val LightOnTertiaryContainer = Color(0xFF3B0A10)
val LightError = ErrorRed
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)
val LightBackground = WarmIvory
val LightOnBackground = Charcoal
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Charcoal
val LightSurfaceVariant = Color(0xFFF1EBDF)
val LightOnSurfaceVariant = StoneGrey
val LightOutline = Color(0xFFA8A496)
val LightOutlineVariant = LightGrey
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFDFBF5)
val LightSurfaceContainer = Color(0xFFF8F3E9)
val LightSurfaceContainerHigh = Color(0xFFF1EBDF)
val LightSurfaceContainerHighest = Color(0xFFEAE3D5)
val LightInverseSurface = Charcoal
val LightInverseOnSurface = Color(0xFFF6F1E8)
val LightInversePrimary = Color(0xFF9CCFBA)

// Dark theme: background #0F1F1A · surface #1B2E28 · card #253C34 · text #F2F3F7 / #B9C1D0
val DarkPrimary = KenteGold
val DarkOnPrimary = Color(0xFF2A1E05)
val DarkPrimaryContainer = Color(0xFF3E3413)
val DarkOnPrimaryContainer = Color(0xFFF6E1B0)
val DarkSecondary = Color(0xFFE4C27E)
val DarkOnSecondary = Color(0xFF402E00)
val DarkSecondaryContainer = Color(0xFF5C4300)
val DarkOnSecondaryContainer = Color(0xFFF6E1B0)
val DarkTertiary = Color(0xFFE5A8B0)
val DarkOnTertiary = Color(0xFF4E1520)
val DarkTertiaryContainer = Color(0xFF5C1F2A)
val DarkOnTertiaryContainer = Color(0xFFFBD9DC)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFF9DEDC)
val DarkBackground = Color(0xFF0F1F1A)
val DarkOnBackground = Color(0xFFF2F3F7)
val DarkSurface = Color(0xFF1B2E28)
val DarkOnSurface = Color(0xFFF2F3F7)
val DarkSurfaceVariant = Color(0xFF253C34)
val DarkOnSurfaceVariant = Color(0xFFB9C1D0)
val DarkOutline = Color(0xFF5F7169)
val DarkOutlineVariant = Color(0xFF31463E)
val DarkSurfaceContainerLowest = Color(0xFF0A1712)
val DarkSurfaceContainerLow = Color(0xFF142620)
val DarkSurfaceContainer = Color(0xFF1B2E28)
val DarkSurfaceContainerHigh = Color(0xFF253C34)
val DarkSurfaceContainerHighest = Color(0xFF2E4740)
val DarkInverseSurface = Color(0xFFF2F3F7)
val DarkInverseOnSurface = Color(0xFF252722)
val DarkInversePrimary = ForestGreen
