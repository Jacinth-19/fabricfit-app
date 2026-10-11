package com.ntoma.studio

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** English-only launch. Legacy preferences must not keep an unreviewed locale selected. */
object AppLocale {
    private const val PREFS = "ntoma_locale"
    private const val KEY = "tag"

    fun currentTag(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY, "en") != "en") {
            prefs.edit().putString(KEY, "en").apply()
        }
        return "en"
    }

    fun apply(context: Context, tag: String) {
        require(tag == "en") { "Only English is available in this release." }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
    }

    fun wrap(base: Context): Context {
        currentTag(base) // Normalize any preference left by an older multilingual build.
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.ENGLISH)
        config.setLayoutDirection(Locale.ENGLISH)
        return base.createConfigurationContext(config)
    }
}
