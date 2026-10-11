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
        try {
            compose.waitUntil(45_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Exception) {
            device.dumpWindowHierarchy(File(artifacts, "failure-ui.xml"))
            device.takeScreenshot(File(artifacts, "failure.png"))
            throw e
        }
    }
    private fun waitText(text: String) {
        try {
            compose.waitUntil(45_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Exception) {
            compose.onRoot().printToLog("NtomaJourney")
            device.dumpWindowHierarchy(File(artifacts, "failure-ui.xml"))
            device.takeScreenshot(File(artifacts, "failure.png"))
            throw e
        }
    }
    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(3000)
        compose.waitForIdle()
        // Hold real screens long enough to read in the emulator recording.
        Thread.sleep(1800)
        assertTrue(device.takeScreenshot(File(artifacts, "$name.png")))
    }

    @Test fun selectedDressFabricFlowsToRealTextureTemplates() {
        val container = AppContainer.get(context)
        runBlocking { container.tooltips.markSeen(TooltipStore.CREATE) }
        val fixture = context.assets.open("editorial/adowa-kente.jpg").use { BitmapFactory.decodeStream(it) }!!
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
            waitTag("studio_editorial_hero")
            screenshot("00-home-light")
            compose.onNodeWithTag("studio_theme_toggle").performClick()
            compose.waitUntil(10_000) { runBlocking { container.settings.current().themeMode } == com.ntoma.studio.domain.model.ThemeMode.DARK }
            screenshot("00-home-dark")
            val darkCapture = BitmapFactory.decodeFile(File(artifacts, "00-home-dark.png").absolutePath)
            val lightCapture = BitmapFactory.decodeFile(File(artifacts, "00-home-light.png").absolutePath)
            assertNotEquals("Theme visibly changes screen pixels", lightCapture.getPixel(8, 200), darkCapture.getPixel(8, 200))
            darkCapture.recycle(); lightCapture.recycle()
            compose.onNodeWithTag("studio_theme_toggle").performClick()
            compose.waitUntil(10_000) { runBlocking { container.settings.current().themeMode } == com.ntoma.studio.domain.model.ThemeMode.LIGHT }
            waitText(context.getString(R.string.nav_create))
            compose.onNodeWithContentDescription(context.getString(R.string.nav_create)).performClick()
            waitText(context.getString(R.string.create_option_gallery_title))
            screenshot("01-create")
            compose.onNodeWithText(context.getString(R.string.create_option_gallery_title)).performClick()
            device.waitForIdle()
            Thread.sleep(2000)
            device.dumpWindowHierarchy(File(artifacts, "picker-ui.xml"))
            assertTrue("Android photo picker did not open; package=${device.currentPackageName}",
                device.wait(Until.hasObject(By.pkg(java.util.regex.Pattern.compile(".*(providers.media|photopicker|documentsui).*"))), 15_000))
            val thumbnail = device.wait(Until.findObject(By.res(java.util.regex.Pattern.compile(".*:id/icon_thumbnail"))), 15_000)
                ?: device.wait(Until.findObject(By.descContains("Photo taken")), 5000)
            assertNotNull("Picker thumbnail absent: ${device.currentPackageName}", thumbnail)
            thumbnail!!.click()
            // Multi-select variants show an explicit confirmation.
            device.findObject(By.text(java.util.regex.Pattern.compile("(?i)(add|open|select).*")))?.click()
            waitTag("fabric_crop_canvas")
            screenshot("02-whole-dress")
            val node = compose.onNodeWithTag("fabric_crop_canvas").fetchSemanticsNode()
            val fit = FittedImage.inView(fixture.width, fixture.height, node.size.width.toFloat(), node.size.height.toFloat())
            compose.onNodeWithTag("fabric_crop_canvas").performTouchInput {
                swipe(Offset(fit.left + fit.width * .36f, fit.top + fit.height * .30f),
                    Offset(fit.left + fit.width * .51f, fit.top + fit.height * .43f), 1000)
            }
            screenshot("03-selected-fabric")
            compose.onNodeWithTag("confirm_fabric_crop").performClick()
            waitText(context.getString(R.string.template_action))
            val saved = runBlocking { container.fabricAnalysis.observeAll().first().first() }
            val loaded = container.imageProcessor.load(android.net.Uri.parse(saved.imageUri), 1600)
            assertTrue(loaded is com.ntoma.studio.domain.model.Outcome.Success)
            val crop = (loaded as com.ntoma.studio.domain.model.Outcome.Success).data
            assertTrue("Actual selected crop width ${crop.width}", crop.width in (fixture.width * .11f).toInt()..(fixture.width * .17f).toInt())
            assertTrue("Actual selected crop height ${crop.height}", crop.height in (fixture.height * .10f).toInt()..(fixture.height * .15f).toInt())
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
            File(artifacts, "validation.txt").writeText("PASS: installed debug app, Android photo picker, whole-dress rectangle selection, persisted selected pixels, local analysis, two outfit templates, repeat scale and Android share sheet. Fixture is the real Adowa Kente photograph by Bright Kwame Ayisi (Kwameghana), CC BY-SA 4.0. Light/dark home captures and persisted theme switches verified. No cloud configuration or trained recognition claims.\n")
        }
        context.contentResolver.delete(uri, null, null)
    }

}
