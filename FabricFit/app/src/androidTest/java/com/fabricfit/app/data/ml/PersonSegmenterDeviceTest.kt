package com.fabricfit.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ON-DEVICE QA for the bundled segmentation models — the piece no JVM test can cover.
 *
 * Run on a real phone (GPU delegate, native libs, real memory pressure):
 *   ./gradlew :app:connectedDebugAndroidTest
 * or filtered:
 *   ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fabricfit.app.data.ml.PersonSegmenterDeviceTest
 *
 * The host-side contract + math QA lives in tools/qa_deeplabv3.py; this exercises the exact
 * production code path (PersonSegmenter.personMask) on device hardware.
 *
 * The subject is a real photograph (androidTest/assets/qa_person.jpg, the same image the
 * try-on E2E uses) rather than a painted rectangle. The previous version drew a tan blob on a
 * blue sky, which is a poor input for a person segmenter trained on photographs: it found few
 * or no person pixels, its own coverage guard returned null, and the test reported "TFLite
 * failed on this device" when in fact the model was working correctly and the *fixture* was
 * wrong. A QA test that fails for its own reasons is worse than no test.
 */
@RunWith(AndroidJUnit4::class)
class PersonSegmenterDeviceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Loads the fixture from the *test* APK's assets, not the app's.
     *
     * src/androidTest/assets is packaged into the instrumentation APK
     * (com.fabricfit.app.debug.test), so it is reachable only through the instrumentation
     * context. ApplicationProvider returns the app under test, whose assets are the shipping
     * ones - asking it for qa_person.jpg threw FileNotFoundException while the file was there.
     */
    private fun photo(): Bitmap {
        val stream = InstrumentationRegistry.getInstrumentation().context.assets.open("qa_person.jpg")
        val bitmap = stream.use { BitmapFactory.decodeStream(it) }
        assertNotNull("qa_person.jpg is in androidTest/assets but could not be decoded", bitmap)
        return bitmap!!
    }

    @Test
    fun segmenterFindsAPersonInARealPhotograph() {
        val segmenter = PersonSegmenter(context)
        val src = photo()

        val t0 = System.nanoTime()
        val mask = segmenter.personMask(src, 240, 320)
        val ms = (System.nanoTime() - t0) / 1_000_000

        // Distinguish the two ways this can fail, because they have different causes and the
        // old message conflated them: the model refusing to load (a packaging/delegate issue)
        // versus the model running and finding nobody (a fixture or threshold issue).
        assertNotNull(
            "personMask returned null. Either no interpreter loaded (the .tflite assets are " +
                "missing from this build - device-qa fetches them before running) or every model " +
                "found fewer person pixels than its coverage guard requires.",
            mask,
        )
        assertEquals(240, mask!!.width)
        assertEquals(320, mask.height)

        val pixels = IntArray(mask.width * mask.height)
        mask.getPixels(pixels, 0, mask.width, 0, 0, mask.width, mask.height)
        val covered = pixels.count { (it ushr 24) and 0xFF > 0 }
        val coverage = covered * 100.0 / pixels.size

        // A full-body photo should be a meaningful fraction of the frame. The guard above
        // already rejects ~0, so this checks we are not getting a sliver that slipped through.
        assertTrue(
            "person coverage implausibly low (%.1f%%) for a full-body photo".format(coverage),
            coverage >= 8.0,
        )
        assertTrue(
            "person coverage implausibly high (%.1f%%) - the mask is probably inverted".format(coverage),
            coverage <= 85.0,
        )

        println("PersonSegmenter: ${ms}ms for ${src.width}x${src.height} -> 240x320, coverage %.1f%%".format(coverage))
        assertTrue("inference suspiciously slow: ${ms}ms", ms < 15_000)
        src.recycle()
        mask.recycle()
    }

    @Test
    fun backgroundPixelsContractHoldsForFullSizeMasks() {
        val w = 64; val h = 64
        val px = IntArray(w * h) { 0xFF336699.toInt() }
        val mask = IntArray(w * h) { if (it % 2 == 0) 0xFF000000.toInt() else 0 }
        val out = PersonSegmenter.backgroundPixels(px, mask)
        for (i in px.indices) {
            val alpha = out[i] ushr 24
            assertEquals(if (i % 2 == 0) 0 else 255, alpha)
            assertEquals("rgb must survive", 0x99, out[i] and 0xFF)
        }
    }
}
