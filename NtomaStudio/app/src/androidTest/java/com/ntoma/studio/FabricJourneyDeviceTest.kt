package com.ntoma.studio

import android.content.ContentValues
import android.graphics.*
import android.provider.MediaStore
import android.os.Environment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.ntoma.studio.di.AppContainer
import com.ntoma.studio.data.local.prefs.TooltipStore
import com.ntoma.studio.media.FittedImage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs the installed app through Android's real picker, crop, analysis, templates and share sheet. */
@RunWith(AndroidJUnit4::class)
class FabricJourneyDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val artifacts = File(context.getExternalFilesDir(null), "journey").apply { mkdirs() }

    private fun waitTag(tag: String) {
        compose.waitUntil(45_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun waitText(text: String) {
        compose.waitUntil(45_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        // Hold real screens long enough to read in the emulator recording.
        Thread.sleep(1800)
        assertTrue(device.takeScreenshot(File(artifacts, "$name.png")))
    }

    @Test fun selectedDressFabricFlowsToRealTextureTemplates() {
        val container = AppContainer.get(context)
        runBlocking { container.tooltips.markSeen(TooltipStore.CREATE) }
        val fixture = dressIllustration()
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ntoma_test_dress.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NtomaQA")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        })!!
        context.contentResolver.openOutputStream(uri)!!.use { fixture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(2800)
            waitText(context.getString(R.string.onboarding_cta_skip))
            compose.onNodeWithText(context.getString(R.string.onboarding_cta_skip)).performClick()
            waitText(context.getString(R.string.nav_create))
            compose.onNodeWithText(context.getString(R.string.nav_create)).performClick()
            waitText(context.getString(R.string.create_option_gallery_title))
            screenshot("01-create")
            compose.onNodeWithText(context.getString(R.string.create_option_gallery_title)).performClick()
            assertTrue("Android photo picker did not open", device.wait(Until.hasObject(By.pkg("com.google.android.providers.media.module")), 15_000) ||
                device.wait(Until.hasObject(By.pkg("com.android.providers.media.module")), 5_000))
            val thumbnail = device.wait(Until.findObject(By.res(java.util.regex.Pattern.compile(".*:id/icon_thumbnail"))), 15_000)
                ?: device.wait(Until.findObject(By.descContains("Photo taken")), 5000)
            assertNotNull("Picker thumbnail absent: ${device.currentPackageName}", thumbnail)
            thumbnail!!.click()
            // Multi-select variants show an explicit confirmation.
            device.findObject(By.text(java.util.regex.Pattern.compile("(?i)(add|open|select).*")))?.click()
            waitTag("fabric_crop_canvas")
            screenshot("02-whole-dress")
            val node = compose.onNodeWithTag("fabric_crop_canvas").fetchSemanticsNode()
            val fit = FittedImage.inView(600, 900, node.size.width.toFloat(), node.size.height.toFloat())
            compose.onNodeWithTag("fabric_crop_canvas").performTouchInput {
                swipe(Offset(fit.left + fit.width * .40f, fit.top + fit.height * .40f),
                    Offset(fit.left + fit.width * .60f, fit.top + fit.height * .60f), 1000)
            }
            screenshot("03-selected-fabric")
            compose.onNodeWithTag("confirm_fabric_crop").performClick()
            waitText(context.getString(R.string.template_action))
            val saved = runBlocking { container.fabricAnalysis.observeAll().first().first() }
            val loaded = container.imageProcessor.load(android.net.Uri.parse(saved.imageUri), 1600)
            assertTrue(loaded is com.ntoma.studio.domain.model.Outcome.Success)
            val crop = (loaded as com.ntoma.studio.domain.model.Outcome.Success).data
            assertTrue("Actual selected crop width ${crop.width}", crop.width in 100..135)
            assertTrue("Actual selected crop height ${crop.height}", crop.height in 150..195)
            assertTrue("Retains pattern pixels", crop.getPixel(20, 20) != crop.getPixel(35, 35))
            crop.recycle()
            screenshot("04-analysis")
            compose.onNodeWithText(context.getString(R.string.template_action)).performScrollTo().performClick()
            waitTag("template_preview")
            screenshot("05-template")
            val styles = runBlocking { container.dressStyles.all() }
            assertTrue(styles.size > 1)
            compose.onNodeWithTag("template_style_${styles[1].id}").performScrollTo().performClick()
            waitTag("template_preview")
            screenshot("06-another-outfit")
            compose.onNodeWithTag("template_more_repeats").performClick()
            waitTag("template_preview")
            screenshot("07-texture-scale")
            compose.onNodeWithTag("share_template").performClick()
            assertTrue("Share sheet did not open", device.wait(Until.hasObject(By.textContains("Sharing")), 10_000) ||
                device.wait(Until.hasObject(By.textContains("Nearby")), 5000) ||
                device.wait(Until.hasObject(By.res("android:id/resolver_list")), 5000))
            assertTrue("Template exported", File(context.filesDir, "media").listFiles()!!.any { it.name.startsWith("template_") && it.length() > 1000 })
            device.pressBack()
            File(artifacts, "validation.txt").writeText("PASS: installed debug app, Android photo picker, whole-dress rectangle selection, persisted selected pixels, local analysis, two outfit templates, repeat scale and Android share sheet. Fixture is an original synthetic dress illustration, not a real photograph. No cloud configuration or trained recognition claims.\n")
        }
        context.contentResolver.delete(uri, null, null)
    }

    private fun dressIllustration(): Bitmap {
        val b = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(Color.rgb(242, 234, 219))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.rgb(97, 61, 42)
        c.drawCircle(300f, 115f, 62f, p)
        c.drawRoundRect(135f, 235f, 185f, 525f, 24f, 24f, p)
        c.drawRoundRect(415f, 235f, 465f, 525f, 24f, 24f, p)
        val garment = Path().apply {
            moveTo(225f, 190f); lineTo(375f, 190f); lineTo(410f, 300f)
            lineTo(365f, 345f); lineTo(450f, 790f); lineTo(150f, 790f)
            lineTo(235f, 345f); lineTo(190f, 300f); close()
        }
        c.save(); c.clipPath(garment)
        val palette = intArrayOf(Color.rgb(202, 147, 40), Color.rgb(29, 86, 79), Color.rgb(153, 47, 52))
        for (y in 0..900 step 24) for (x in 0..600 step 24) {
            p.color = palette[(x / 24 + y / 24) % 3]
            c.drawRect(x.toFloat(), y.toFloat(), x + 24f, y + 24f, p)
            p.color = Color.rgb(247, 224, 156)
            c.drawCircle(x + 12f, y + 12f, 3f, p)
        }
        c.restore()
        p.color = Color.rgb(31, 27, 22); p.style = Paint.Style.STROKE; p.strokeWidth = 3f
        c.drawPath(garment, p)
        p.style = Paint.Style.FILL; p.textSize = 20f
        c.drawText("ORIGINAL TEST ILLUSTRATION", 134f, 850f, p)
        c.drawText("Select the fabric, not the background", 118f, 879f, p)
        return b
    }
}
