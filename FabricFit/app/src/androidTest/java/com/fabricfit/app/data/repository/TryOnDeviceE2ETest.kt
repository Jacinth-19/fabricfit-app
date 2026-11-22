package com.fabricfit.app.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fabricfit.app.data.ml.PersonSegmenter
import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.Outcome
import com.fabricfit.app.domain.repository.GenerationInput
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the full FitRoom try-on on a real handset and writes the artifacts to external
 * storage so CI can publish them:
 *
 *   adb shell am instrument -w -e class com.fabricfit.app.data.repository.TryOnDeviceE2ETest \
 *       com.fabricfit.app.debug/androidx.test.runner.AndroidJUnitRunner
 *
 * The pipeline under test is exactly what the app runs:
 *   DemoTryOnSamples (Ghanaian model + kente swatch bought online)
 *     -> ImageProcessor.load -> PersonSegmenter.personMask (Selfie/DeepLabV3 TFLite)
 *     -> GarmentGeometry polygons -> DemoLookGenerator composite -> saved JPEG
 */
@RunWith(AndroidJUnit4::class)
class TryOnDeviceE2ETest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun bundledGhanaianModelAndKenteSwatchProduceATryOn() {
        // Write to shared Downloads: `adb pull /sdcard/Android/data/...` cannot read the
        // app's external files dir through the shell on API 30+ (scoped storage), which is
        // why the first emulator runs reported "no files". App-private storage is the
        // fallback when no external volume is mounted.
        val outDir = run {
            val downloads = File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS,
            ), "FabricFit/vton-e2e")
            val dir = if (downloads.mkdirs() || downloads.isDirectory) downloads else
                File(context.getExternalFilesDir(null) ?: context.filesDir, "vton-e2e")
            dir.apply { mkdirs() }
        }
        // Print the resolved path so CI logs show where the files actually went.
        println("VTON_OUT_DIR=${outDir.absolutePath}")
        assertTrue("artifact directory is not writable: $outDir", outDir.isDirectory && outDir.canWrite())

        val personPath = kotlinx.coroutines.runBlocking { DemoTryOnSamples.personPhoto(context) }   // bare path
        val fabricUri = kotlinx.coroutines.runBlocking { DemoTryOnSamples.fabricSwatch(context) }   // file:// uri
        val fabricPath = java.io.File(android.net.Uri.parse(fabricUri).path!!).absolutePath
        assertTrue("model photo missing: $personPath", File(personPath).length() > 10_000)
        assertTrue("fabric swatch missing: $fabricUri", File(fabricPath).length() > 5_000)

        val processor = com.fabricfit.app.media.ImageProcessor(context)
        // Same entry point the app uses for a sample photo: a bare absolute path.
        val loaded = processor.load(personPath, 1000)
        assertTrue(
            "person photo must load: $personPath (exists=${File(personPath).exists()}) -> $loaded",
            loaded is Outcome.Success,
        )
        val person = (loaded as Outcome.Success).data

        // The real device path for the segmenter: DeepLabV3 first, Selfie fallback.
        val segmenter = PersonSegmenter(context)
        val t0 = System.nanoTime()
        val mask = segmenter.personMask(person, person.width, person.height)
        val maskMs = (System.nanoTime() - t0) / 1_000_000
        assertNotNull("segmentation failed on device", mask)
        mask!!.let {
            val cover = coverage(it)
            assertTrue("mask covers $cover of the frame — expected 2%..80%", cover in 0.02..0.80)
            it.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(File(outDir, "person_mask.png")))
        }

        val generator = DemoLookGenerator(processor, segmenter)
        val fabric = Fabric(
            imageUri = fabricUri,
            name = DemoTryOnSamples.FABRIC_NAME,
            category = com.fabricfit.app.domain.model.FabricCategory.KENTE,
            colors = listOf(
                com.fabricfit.app.domain.model.AnalyzedColor(
                    0xFFF2C230L,
                    com.fabricfit.app.domain.model.ColorName.GOLD,
                    0.34f,
                ),
                com.fabricfit.app.domain.model.AnalyzedColor(
                    0xFF2FA7D8L,
                    com.fabricfit.app.domain.model.ColorName.BLUE,
                    0.28f,
                ),
                com.fabricfit.app.domain.model.AnalyzedColor(
                    0xFF2E7D32L,
                    com.fabricfit.app.domain.model.ColorName.GREEN,
                    0.22f,
                ),
                com.fabricfit.app.domain.model.AnalyzedColor(
                    0xFF14110FL,
                    com.fabricfit.app.domain.model.ColorName.BLACK,
                    0.16f,
                ),
            ),
            pattern = com.fabricfit.app.domain.model.PatternType.STRIPED,
            texture = com.fabricfit.app.domain.model.TextureType.WOVEN,
            confidence = 0.93f,
            createdAt = System.currentTimeMillis(),
        )
        val style = DressStyle(
            id = "kaba_slit",
            titleKey = "style_kaba_slit",
            descriptionKey = "style_kaba_slit_desc",
            gender = com.fabricfit.app.domain.model.GenderCategory.WOMEN,
            category = com.fabricfit.app.domain.model.StyleCategory.TRADITIONAL,
            occasions = setOf(com.fabricfit.app.domain.model.Occasion.CHURCH),
            silhouette = com.fabricfit.app.domain.model.GarmentSilhouette.KABA,
            affinity = setOf(com.fabricfit.app.domain.model.FabricCategory.KENTE),
            structure = 0.65f,
        )

        val t1 = System.nanoTime()
        val outcome = kotlinx.coroutines.runBlocking {
            generator.generate(
                GenerationInput(
                    fabric = fabric,
                    style = style,
                    personImagePath = personPath,
                    customization = DesignCustomization(
                        sleeve = com.fabricfit.app.domain.model.SleeveOption.PUFF,
                        neckline = com.fabricfit.app.domain.model.NecklineOption.VNECK,
                        length = com.fabricfit.app.domain.model.LengthOption.MAXI,
                    ),
                    maxDimension = 1000,
                ),
            )
        }
        val generateMs = (System.nanoTime() - t1) / 1_000_000
        assertTrue("generation failed: $outcome", outcome is Outcome.Success)
        val file = (outcome as Outcome.Success).data
        assertTrue("result file missing", file.exists() && file.length() > 20_000)

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        assertNotNull(bitmap)
        bitmap!!
        // ImageProcessor.save() caps the long edge at SAVE_MAX_EDGE (1280 px), so the saved
        // JPEG keeps the source aspect ratio but may be smaller than the composited bitmap.
        val sourceRatio = person.width.toFloat() / person.height
        val savedRatio = bitmap.width.toFloat() / bitmap.height
        assertEquals("saved look must keep the source aspect ratio", sourceRatio, savedRatio, 0.01f)
        assertTrue(
            "saved look should be a real-size photo, got ${bitmap.width}x${bitmap.height}",
            maxOf(bitmap.width, bitmap.height) in 900..com.fabricfit.app.media.ImageProcessor.SAVE_MAX_EDGE,
        )

        // Persist artifacts for CI upload.
        file.copyTo(File(outDir, "tryon_result.jpg"), overwrite = true)
        person.compress(Bitmap.CompressFormat.JPEG, 88, FileOutputStream(File(outDir, "person_input.jpg")))
        File(fabricPath).copyTo(File(outDir, "fabric_swatch.jpg"), overwrite = true)

        val report = buildString {
            appendLine("FabricFit on-device try-on E2E")
            appendLine("device      ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("model       ${DemoTryOnSamples.MODEL_ASSET}")
            appendLine("dress       ${DemoTryOnSamples.DRESS_ASSET} — ${DemoTryOnSamples.SOURCE_URL}")
            appendLine("segment     ${maskMs}ms, coverage ${"%.1f".format(coverage(mask) * 100)}%")
            appendLine("generate    ${generateMs}ms")
            appendLine("result      ${file.absolutePath} (${file.length() / 1024} KB, ${bitmap.width}x${bitmap.height})")
            appendLine("artifacts   ${outDir.absolutePath}")
        }
        File(outDir, "report.txt").writeText(report)
        println(report)
    }

    private fun coverage(mask: Bitmap): Double {
        var person = 0
        val row = IntArray(mask.width)
        for (y in 0 until mask.height) {
            mask.getPixels(row, 0, mask.width, 0, y, mask.width, 1)
            for (px in row) if (((px ushr 24) and 0xFF) > 128) person++
        }
        return person.toDouble() / (mask.width.toLong() * mask.height)
    }
}
