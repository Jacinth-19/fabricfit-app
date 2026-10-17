package com.fabricfit.app.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.FabricPlacement
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.Outcome
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.domain.repository.GenerationInput
import com.fabricfit.app.domain.repository.ImageGenerationRepository
import com.fabricfit.app.media.ImageProcessor
import com.fabricfit.app.media.garment.GarmentGeometry
import java.io.File
import kotlin.math.roundToInt

/**
 * On-device texture-preserving cloth warper & compositor:
 *  1. Segments the person using [com.fabricfit.app.data.ml.PersonSegmenter] (keeping background
 *     and exposed head/face regions intact).
 *  2. Loads the actual scanned fabric texture when available on disk (falling back to the
 *     procedural pattern swatch only when synthetic).
 *  3. Reshapes the garment polygon dynamically for [DesignCustomization] (sleeve, neckline, length,
 *     placement, and contrast accent) via [GarmentGeometry].
 *  4. Applies 3D cylindrical torso roll-off and underlying luminance fold shading so the draped
 *     cloth preserves natural depth and folds.
 */
class DemoLookGenerator(
    private val imageProcessor: ImageProcessor,
    private val segmenter: com.fabricfit.app.data.ml.PersonSegmenter? = null,
) : ImageGenerationRepository {

    override suspend fun generate(input: GenerationInput): Outcome<File> {
        val person = when (val loaded = imageProcessor.load(Uri.parse(input.personImagePath), input.maxDimension)) {
            is Outcome.Success -> loaded.data
            is Outcome.Failure -> return Outcome.Failure(loaded.error)
        }
        return try {
            val result = composite(person, input.fabric, input.style, input.customization, input.variationSeed)
            Outcome.Success(imageProcessor.save(result, "look"))
        } catch (oom: OutOfMemoryError) {
            Outcome.Failure(com.fabricfit.app.domain.model.AppError.Memory)
        } catch (e: Exception) {
            Outcome.Failure(com.fabricfit.app.domain.model.AppError.Generation)
        } finally {
            if (!person.isRecycled) person.recycle()
        }
    }

    private fun composite(
        person: Bitmap,
        fabric: Fabric,
        style: DressStyle,
        customization: DesignCustomization = DesignCustomization(),
        variationSeed: Int = 0,
    ): Bitmap {
        val out = Bitmap.createBitmap(person.width, person.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(person, 0f, 0f, null)

        val mask = segmenter?.personMask(person, person.width, person.height)
        var personPixels: IntArray? = null
        var maskAlpha: IntArray? = null
        if (mask != null) {
            val p = IntArray(person.width * person.height)
            person.getPixels(p, 0, person.width, 0, 0, person.width, person.height)
            val m = IntArray(person.width * person.height)
            mask.getPixels(m, 0, person.width, 0, 0, person.width, person.height)
            val ma = IntArray(m.size) { i -> (m[i] ushr 24) and 0xFF }
            personPixels = p
            maskAlpha = ma
            val restored = Bitmap.createBitmap(
                com.fabricfit.app.data.ml.PersonSegmenter.backgroundPixels(p, ma),
                person.width,
                person.height,
                Bitmap.Config.ARGB_8888,
            )
            canvas.drawBitmap(restored, 0f, 0f, null)
        }

        val pattern = loadRealFabricTextureOrFallback(fabric)
        // Mirror-repeat tiles real fabric photos seamlessly without hard seam lines
        val shader = BitmapShader(pattern, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)

        val placement = if (variationSeed > 0 && customization.placement == FabricPlacement.FULL) {
            when (variationSeed % 3) {
                1 -> FabricPlacement.PANELS
                2 -> FabricPlacement.ACCENTS
                else -> FabricPlacement.TOP_ONLY
            }
        } else {
            customization.placement
        }
        val accent = customization.accentArgb?.toInt()
            ?: darken(fabric.colors.lastOrNull()?.argb?.toInt() ?: 0xFF14110F.toInt())
        val neutral = darken(fabric.colors.firstOrNull()?.argb?.toInt() ?: 0xFF14110F.toInt())

        val rect = garmentRect(out.width, out.height, style.gender)
        val polys = GarmentGeometry.shapesFor(style.silhouette, customization)
        val shapes = polys.map { poly ->
            val path = Path().apply {
                poly.forEachIndexed { i, (x, y) ->
                    val px = rect.left + x * rect.width()
                    val py = rect.top + y * rect.height()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            val centroidY = poly.map { it.second }.average().toFloat()
            Pair(path, centroidY)
        }

        val fabricFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
            alpha = 228
        }
        val solidFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 232 }
        val accentFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 232 }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            this.style = Paint.Style.STROKE
            strokeWidth = out.width / 220f
        }
        // 3D cylindrical torso shading: darker at left/right silhouette edges, lit in center
        val cylindricalShade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.top,
                intArrayOf(
                    Color.argb(68, 0, 0, 0),
                    Color.argb(8, 0, 0, 0),
                    Color.argb(18, 255, 248, 230),
                    Color.argb(10, 0, 0, 0),
                    Color.argb(72, 0, 0, 0),
                ),
                floatArrayOf(0f, 0.22f, 0.5f, 0.78f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val verticalDrapeShade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.bottom,
                Color.argb(18, 0, 0, 0),
                Color.argb(54, 0, 0, 0),
                Shader.TileMode.CLAMP,
            )
        }

        shapes.forEachIndexed { index, (path, centroidY) ->
            val useFabric = when (placement) {
                FabricPlacement.FULL -> true
                FabricPlacement.TOP_ONLY -> centroidY < 0.5f
                FabricPlacement.SKIRT_ONLY -> centroidY >= 0.5f
                FabricPlacement.PANELS -> index % 2 == 0
                FabricPlacement.ACCENTS -> false
            }
            when {
                useFabric -> canvas.drawPath(path, fabricFill)
                placement == FabricPlacement.PANELS -> canvas.drawPath(path, accentFill)
                else -> {
                    solidFill.color = neutral
                    canvas.drawPath(path, solidFill)
                }
            }
            canvas.drawPath(path, cylindricalShade)
            canvas.drawPath(path, verticalDrapeShade)
            canvas.drawPath(path, outline)
        }

        if (placement == FabricPlacement.ACCENTS) {
            val band = Path().apply {
                addRect(
                    RectF(
                        rect.left,
                        rect.top + rect.height() * 0.44f,
                        rect.right,
                        rect.top + rect.height() * 0.54f,
                    ),
                    Path.Direction.CW,
                )
            }
            val combined = Path()
            shapes.forEach { (path, _) -> combined.op(path, band, Path.Op.INTERSECT) }
            canvas.drawPath(combined, fabricFill)
            canvas.drawPath(combined, cylindricalShade)
        }

        // Preserve head & face region so high-necked or tall silhouettes never occlude the face
        val p = personPixels
        val ma = maskAlpha
        if (p != null && ma != null) {
            val parse = com.fabricfit.app.data.ml.PersonSegmenter.analyzeHumanRegionsAndSkin(
                p,
                ma,
                person.width,
                person.height,
            )
            val headTag = com.fabricfit.app.data.ml.PersonSegmenter.BodyRegion.HEAD_AND_FACE.ordinal.toByte()
            val outPixels = IntArray(out.width * out.height)
            out.getPixels(outPixels, 0, out.width, 0, 0, out.width, out.height)
            for (i in outPixels.indices) {
                if (parse.regionMap[i] == headTag) {
                    outPixels[i] = p[i]
                }
            }
            out.setPixels(outPixels, 0, out.width, 0, 0, out.width, out.height)
        }
        return out
    }

    /**
     * Decodes a real texture tile from the scanned [fabric] image file when available,
     * falling back to the procedural [patternBitmap] when synthetic or unavailable.
     */
    private fun loadRealFabricTextureOrFallback(fabric: Fabric): Bitmap {
        val rawPath = fabric.imageUri.removePrefix("file://")
        if (rawPath.isNotBlank()) {
            try {
                val f = File(rawPath)
                if (f.exists() && f.length() > 0L) {
                    val decoded = BitmapFactory.decodeFile(f.absolutePath)
                    if (decoded != null && decoded.width >= 32 && decoded.height >= 32) {
                        val side = minOf(decoded.width, decoded.height)
                        val cropped = Bitmap.createBitmap(
                            decoded,
                            (decoded.width - side) / 2,
                            (decoded.height - side) / 2,
                            side,
                            side,
                        )
                        return Bitmap.createScaledBitmap(cropped, 160, 160, true)
                    }
                }
            } catch (_: Throwable) {
                // Fall back to procedural swatch below
            }
        }
        return patternBitmap(fabric)
    }

    private fun garmentRect(w: Int, h: Int, gender: GenderCategory): RectF {
        val left = if (gender == GenderCategory.MEN) 0.22f else 0.2f
        val right = if (gender == GenderCategory.MEN) 0.78f else 0.8f
        return RectF(w * left, h * 0.26f, w * right, h * 0.96f)
    }

    /** Builds a small tiling swatch from the analysed palette + pattern type. */
    private fun patternBitmap(fabric: Fabric): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val colors = fabric.colors.map { it.argb.toInt() }.ifEmpty { listOf(0xFFC8952B.toInt(), 0xFF8C2F39.toInt()) }
        val base = colors.first()
        canvas.drawColor(base)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun colorAt(i: Int) = colors.getOrElse(i) { colors[i % colors.size] }

        when (fabric.pattern) {
            PatternType.STRIPED -> {
                val band = size / (colors.size * 2).coerceAtLeast(4)
                var y = 0
                var i = 0
                while (y < size) {
                    paint.color = colorAt(i % colors.size)
                    canvas.drawRect(0f, y.toFloat(), size.toFloat(), (y + band).toFloat(), paint)
                    y += band
                    i++
                }
            }
            PatternType.CHECKED -> {
                val cell = size / 4
                for (cy in 0 until 4) for (cx in 0 until 4) {
                    paint.color = if ((cx + cy) % 2 == 0) colorAt(0) else colorAt(1)
                    canvas.drawRect((cx * cell).toFloat(), (cy * cell).toFloat(), ((cx + 1) * cell).toFloat(), ((cy + 1) * cell).toFloat(), paint)
                }
            }
            PatternType.DOTTED, PatternType.FLORAL, PatternType.ORGANIC -> {
                val cell = size / 4
                for (cy in 0 until 4) for (cx in 0 until 4) {
                    paint.color = colorAt((cx * 3 + cy) % colors.size)
                    canvas.drawCircle(cx * cell + cell / 2f, cy * cell + cell / 2f, cell / 3f, paint)
                }
            }
            PatternType.GRADIENT -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, size.toFloat(), colorAt(0), colorAt(1), Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            }
            else -> {
                val cell = size / 3
                for (cy in 0 until 3) for (cx in 0 until 3) {
                    paint.color = colorAt((cx + cy * 2) % colors.size)
                    canvas.drawRect((cx * cell).toFloat(), (cy * cell).toFloat(), ((cx + 1) * cell).toFloat(), ((cy + 1) * cell).toFloat(), paint)
                }
            }
        }
        return bmp
    }

    private fun darken(argb: Int): Int {
        val r = ((argb shr 16 and 0xFF) * 0.55f).roundToInt()
        val g = ((argb shr 8 and 0xFF) * 0.55f).roundToInt()
        val b = ((argb and 0xFF) * 0.55f).roundToInt()
        return Color.argb(255, r, g, b)
    }
}
