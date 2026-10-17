package com.fabricfit.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

/**
 * Heritage Luxe custom branded font family.
 * Uses Playfair Display as the premium serif font for brand identity.
 * 
 * To use custom .ttf/.otf fonts instead:
 * 1. Add font files to res/font/ directory
 * 2. Replace GoogleFont with Font(R.font.your_font_name)
 */
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val HeritageLuxeFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Playfair Display"),
        fontProvider = provider,
    )
)

/**
 * Fallback font family if Google Fonts fail to load.
 */
val FallbackSerifFontFamily = FontFamily.Serif
