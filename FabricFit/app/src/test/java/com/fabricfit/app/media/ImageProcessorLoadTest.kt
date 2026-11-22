package com.fabricfit.app.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.fabricfit.app.domain.model.Outcome
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for how the FitRoom loads a person photo.
 *
 * The on-device E2E failed with "person photo must load" because the sample model is passed
 * as a bare absolute path and `Uri.parse(path)` does not give back a usable `path` on every
 * API level. These tests pin both spellings so a refactor cannot break the phone again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageProcessorLoadTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val processor = ImageProcessor(context)

    /** Writes a real JPEG so the two-pass decode has something to read. */
    private fun jpegFile(name: String, w: Int = 64, h: Int = 96): File {
        val dir = File(context.filesDir, "demo").apply { mkdirs() }
        val file = File(dir, name)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF3355FF.toInt())
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file
    }

    @Test
    fun `loads a bare absolute path`() {
        val file = jpegFile("bare.jpg")
        val outcome = processor.load(file.absolutePath, 1000)
        assertTrue("bare path failed: $outcome", outcome is Outcome.Success)
        assertEquals(64, (outcome as Outcome.Success).data.width)
        assertEquals(96, outcome.data.height)
    }

    @Test
    fun `loads a file uri`() {
        val file = jpegFile("uri.jpg")
        val outcome = processor.load("file://${file.absolutePath}", 1000)
        assertTrue("file uri failed: $outcome", outcome is Outcome.Success)
        assertEquals(64, (outcome as Outcome.Success).data.width)
    }

    @Test
    fun `downsamples photos larger than the requested edge`() {
        val file = jpegFile("large.jpg", 1200, 1600)
        val outcome = processor.load(file.absolutePath, 400)
        assertTrue(outcome is Outcome.Success)
        val bitmap = (outcome as Outcome.Success).data
        // inSampleSize is a power of two, so 1600 -> 400 exactly (sample 4).
        assertEquals(300, bitmap.width)
        assertEquals(400, bitmap.height)
    }

    @Test
    fun `missing path fails instead of throwing`() {
        val outcome = processor.load("/definitely/not/here.jpg", 1000)
        assertTrue("expected a failure outcome, got $outcome", outcome is Outcome.Failure)
    }

    @Test
    fun `bundled demo asset loads once copied to app storage`() {
        // Mirror what DemoTryOnSamples.personPhoto does, without the coroutine plumbing.
        val out = File(File(context.filesDir, "demo").apply { mkdirs() }, "ama_fullbody.jpg")
        context.assets.open("catalog/samples/model_ama_fullbody.jpg").use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        // The asset is 768x1376; the FitRoom asks for 1000 px, decoded via sample 1.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(out.absolutePath, bounds)
        assertEquals(768, bounds.outWidth)
        assertEquals(1376, bounds.outHeight)

        val outcome = processor.load(out.absolutePath, 1000)
        assertTrue("sample model failed: $outcome", outcome is Outcome.Success)
        val bitmap = (outcome as Outcome.Success).data
        assertEquals(1376, bitmap.height)
    }
}
