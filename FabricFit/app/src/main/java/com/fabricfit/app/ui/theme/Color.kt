package com.fabricfit.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * FabricFit — Heritage Luxe palette.
 * Deep Forest green + Royal Gold on a Warm Cream base, evoking Kente royalty and
 * premium fashion editorial. Not dynamic-colour: the brand reads as a fashion
 * product, not a launcher theme.
 */

// Heritage Luxe brand tokens (theme independent)
val HeritageForest = Color(0xFF0F3D2E)      // primary dark green
val HeritageForestDark = Color(0xFF0A2B20)  // deeper surface
val HeritageGold = Color(0xFFD4A84B)        // royal gold accent
val HeritageGoldSoft = Color(0xFFC8A96A)    // muted gold for secondary text on dark
val HeritageCream = Color(0xFFF7F1E5)       // warm cream background
val HeritageCreamBright = Color(0xFFFBF7EE) // lighter cream
val HeritageInk = Color(0xFF1A1A1A)         // near-black for body on light
val HeritageRed = Color(0xFFB83227)         // Kente red accent
val HeritageBurgundy = Color(0xFF7A1F2B)    // deep burgundy
val HeritageLace = Color(0xFFEADFCB)        // lace / ivory accent
val HeritageBatikBrown = Color(0xFF6B3F1D)  // batik brown

// Light scheme — Warm Cream surfaces with Deep Forest primary + Royal Gold accents
val LightPrimary = HeritageForest
val LightOnPrimary = HeritageCream
val LightPrimaryContainer = Color(0xFFD9E7E0)
val LightOnPrimaryContainer = Color(0xFF052017)
val LightSecondary = HeritageGold
val LightOnSecondary = HeritageInk
val LightSecondaryContainer = Color(0xFFF3E4BE)
val LightOnSecondaryContainer = Color(0xFF3A2A06)
val LightTertiary = HeritageBurgundy
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFF5DAD9)
val LightOnTertiaryContainer = Color(0xFF3A0B12)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)
val LightBackground = HeritageCream
val LightOnBackground = HeritageInk
val LightSurface = HeritageCream
val LightOnSurface = HeritageInk
val LightSurfaceVariant = Color(0xFFEADFCB)
val LightOnSurfaceVariant = Color(0xFF544838)
val LightOutline = Color(0xFF8A7B66)
val LightOutlineVariant = Color(0xFFD8CCB8)
val LightSurfaceContainerLowest = Color(0xFFFFFBF3)
val LightSurfaceContainerLow = Color(0xFFFCF6EA)
val LightSurfaceContainer = Color(0xFFF5EEDF)
val LightSurfaceContainerHigh = Color(0xFFEFE7D5)
val LightSurfaceContainerHighest = Color(0xFFE8DFCA)
val LightInverseSurface = Color(0xFF2B241A)
val LightInverseOnSurface = Color(0xFFF6EEDD)
val LightInversePrimary = Color(0xFFA7D3C1)

// Dark scheme — Deep Forest surfaces with gold + cream type
val DarkPrimary = Color(0xFF7BB79F)
val DarkOnPrimary = Color(0xFF04291D)
val DarkPrimaryContainer = HeritageForest
val DarkOnPrimaryContainer = Color(0xFFB8DCCC)
val DarkSecondary = HeritageGold
val DarkOnSecondary = Color(0xFF3A2A06)
val DarkSecondaryContainer = Color(0xFF584108)
val DarkOnSecondaryContainer = Color(0xFFF6E4AE)
val DarkTertiary = Color(0xFFE7B3B6)
val DarkOnTertiary = Color(0xFF4B131B)
val DarkTertiaryContainer = Color(0xFF68212A)
val DarkOnTertiaryContainer = Color(0xFFFFDAD9)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)
val DarkBackground = HeritageForestDark
val DarkOnBackground = HeritageCream
val DarkSurface = HeritageForestDark
val DarkOnSurface = HeritageCream
val DarkSurfaceVariant = Color(0xFF1C4437)
val DarkOnSurfaceVariant = Color(0xFFC8BFAE)
val DarkOutline = HeritageGoldSoft
val DarkOutlineVariant = Color(0xFF47594F)
val DarkSurfaceContainerLowest = Color(0xFF072018)
val DarkSurfaceContainerLow = Color(0xFF0C2C22)
val DarkSurfaceContainer = Color(0xFF10362B)
val DarkSurfaceContainerHigh = Color(0xFF174033)
val DarkSurfaceContainerHighest = Color(0xFF204B3D)
val DarkInverseSurface = HeritageCream
val DarkInverseOnSurface = HeritageInk
val DarkInversePrimary = HeritageForest

// Legacy brand art accents (retained for artwork/renderers outside MaterialTheme)
val BrandInk = HeritageInk
val BrandCream = HeritageCream
val BrandGold = HeritageGold
val BrandMadder = HeritageBurgundy
val BrandTeal = HeritageForest
