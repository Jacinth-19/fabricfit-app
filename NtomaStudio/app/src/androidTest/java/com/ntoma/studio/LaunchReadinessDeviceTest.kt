package com.ntoma.studio

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ntoma.studio.data.ml.OnDeviceMlClassifier
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/** Runtime checks: real LiteRT JNI loading/inference and safe locale fallback/formatting. */
@RunWith(AndroidJUnit4::class)
class LaunchReadinessDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun localModelRunsOnDeviceWithCurrentPageSize() {
        val source = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        for (y in 0 until 224) for (x in 0 until 224) {
            source.setPixel(x, y, if ((x / 16 + y / 16) % 2 == 0) Color.BLUE else Color.YELLOW)
        }
        val file = File(context.cacheDir, "runtime_test.png")
        try {
            file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val predictions = OnDeviceMlClassifier(context).classify(android.net.Uri.fromFile(file))
            assertTrue("LiteRT must return predictions, not silently swallow a JNI failure", predictions.isNotEmpty())
        } finally {
            source.recycle()
            file.delete()
        }
    }

    @Test fun englishOnlyLaunchNormalizesLegacyLanguageChoices() {
        val englishConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        }
        val english = context.createConfigurationContext(englishConfig)
        for (tag in listOf("tw", "gaa", "ee")) {
            val config = android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            }
            val foreignContext = context.createConfigurationContext(config)
            context.getSharedPreferences("ntoma_locale", android.content.Context.MODE_PRIVATE)
                .edit().putString("tag", tag).commit()
            val localized = AppLocale.wrap(foreignContext)
            assertEquals("en", AppLocale.currentTag(context))
            assertEquals("en", localized.resources.configuration.locales[0].language)
            assertEquals(listOf("en"), com.ntoma.studio.domain.model.LanguageOption.values().map { it.tag })
            for (id in listOf(R.string.template_disclosure, R.string.privacy_device_body, R.string.edit_hint, R.string.nav_create)) {
                assertEquals("$tag must not ship unreviewed workflow copy", english.getString(id), localized.getString(id))
            }
            assertEquals(english.getString(R.string.tryon_quota_note, 3, 5), localized.getString(R.string.tryon_quota_note, 3, 5))
        }
    }
}
