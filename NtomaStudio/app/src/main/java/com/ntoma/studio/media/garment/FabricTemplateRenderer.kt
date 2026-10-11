package com.ntoma.studio.media.garment

import android.graphics.*
import android.net.Uri
import com.ntoma.studio.domain.model.DressStyle
import com.ntoma.studio.domain.model.Outcome
import com.ntoma.studio.media.ImageProcessor

/** Flat, local texture layout. No inferred body, fit, drape, lighting or cloud request. */
class FabricTemplateRenderer(private val images: ImageProcessor) {
    fun render(imageUri: String, style: DressStyle, repeatCount: Int = 3): Outcome<Bitmap> {
        val texture = when (val loaded = images.load(Uri.parse(imageUri), 512)) {
            is Outcome.Success -> loaded.data
            is Outcome.Failure -> return loaded
        }
        return try {
            val w = 600
            val h = 800
            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.rgb(253, 248, 242))
            val shader = BitmapShader(texture, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
            shader.setLocalMatrix(Matrix().apply {
                val scale = w.toFloat() / repeatCount.coerceIn(1, 6) / texture.width
                setScale(scale, scale)
            })
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader; isFilterBitmap = true }
            val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(31, 27, 22); this.style = Paint.Style.STROKE; strokeWidth = 2f
            }
            GarmentGeometry.shapesFor(style.silhouette).forEach { polygon ->
                val path = Path().apply {
                    polygon.forEachIndexed { i, (x, y) ->
                        val px = 30 + x * 540
                        val py = 24 + y * 744
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                canvas.drawPath(path, fill)
                canvas.drawPath(path, outline)
            }
            Outcome.Success(output)
        } catch (e: OutOfMemoryError) {
            Outcome.Failure(com.ntoma.studio.domain.model.AppError.Memory)
        } catch (e: Exception) {
            Outcome.Failure(com.ntoma.studio.domain.model.AppError.Generation)
        } finally { texture.recycle() }
    }
}
