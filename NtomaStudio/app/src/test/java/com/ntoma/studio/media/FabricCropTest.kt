package com.ntoma.studio.media

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FabricCropTest {
    @Test fun `fit letterboxing maps touch to image not padding`() {
        val fit = FittedImage.inView(600, 900, 1000f, 1000f)
        assertEquals(666.6667f, fit.width, .01f)
        assertEquals(.5f, fit.imageX(500f), .001f)
        assertEquals(0f, fit.imageX(0f), 0f)
        assertEquals(1f, fit.imageY(1200f), 0f)
    }
    @Test fun `crop uses exactly the visible rectangle pixels`() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        for (y in 30 until 70) for (x in 20 until 60) source.setPixel(x, y, Color.BLUE)
        val processor = ImageProcessor(ApplicationProvider.getApplicationContext())
        val selected = processor.crop(source, FabricCrop(.2f, .3f, .6f, .7f))
        // Floating-point boundary rounding may add one edge pixel, never shifts the selection.
        assertTrue(selected.width in 40..41)
        assertEquals(Color.BLUE, selected.getPixel(selected.width / 2, selected.height / 2))
    }
    @Test fun `full image includes all pixels and tiny selection stays in bounds`() {
        assertEquals(PixelCrop(0, 0, 123, 456), FabricCrop.FULL.pixels(123, 456))
        assertEquals(PixelCrop(122, 455, 1, 1), FabricCrop(1f, 1f, 1f, 1f).pixels(123, 456))
    }
}
