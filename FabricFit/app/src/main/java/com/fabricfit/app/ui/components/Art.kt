package com.fabricfit.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.media.garment.GarmentGeometry
import com.fabricfit.app.ui.theme.BrandGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FALLBACK = listOf(Color(0xFFC8952B), Color(0xFF8C2F39), Color(0xFF1F5450))

internal fun Fabric.paletteColors(): List<Color> =
    colors.map { Color(it.argb) }.ifEmpty { FALLBACK }

/**
 * Procedural swatch: renders the analysed palette using the detected pattern family, so every
 * fabric card looks like *that* fabric without bundling photos. Finished with a cloth vignette,
 * a diagonal sheen and a hairline frame for a woven, tactile feel.
 */
@Composable
fun FabricSwatch(fabric: Fabric, modifier: Modifier = Modifier) {
    val colors = fabric.paletteColors()
    val pattern = fabric.pattern
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (pattern) {
            PatternType.STRIPED -> {
                drawRect(colors.first())
                val bands = (colors.size * 2).coerceAtLeast(4)
                val band = h / bands
                repeat(bands) { i ->
                    val thickness = if (i % 2 == 0) band else band * 0.45f
                    drawRect(colors[i % colors.size], topLeft = Offset(0f, i * band), size = Size(w, thickness))
                    drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(0f, i * band), size = Size(w, 1f))
                }
            }
            PatternType.CHECKED -> {
                drawRect(colors.first())
                val cell = w / 5
                for (cy in 0 until 5) for (cx in 0 until 5) {
                    if ((cx + cy) % 2 == 0) continue
                    drawRect(colors.getOrElse(1) { colors[0] }, topLeft = Offset(cx * cell, cy * cell), size = Size(cell, cell))
                }
                // cream warp threads over the check
                for (x in 0..5) {
                    drawLine(Color(0xFFFDF8F2).copy(alpha = 0.45f), Offset(x * cell, 0f), Offset(x * cell, h), 1f)
                }
                for (y in 0..5) {
                    drawLine(Color(0xFFFDF8F2).copy(alpha = 0.45f), Offset(0f, y * (h / 5f)), Offset(w, y * (h / 5f)), 1f)
                }
            }
            PatternType.DOTTED, PatternType.FLORAL, PatternType.ORGANIC -> {
                drawRect(colors.first())
                val cell = w / 5
                for (cy in 0 until 6) {
                    val stagger = if (cy % 2 == 0) 0f else cell / 2f
                    for (cx in 0 until 6) {
                        val center = Offset(cx * cell + cell / 2f + stagger, cy * cell + cell / 2f)
                        drawCircle(colors[(cx + cy) % colors.size], radius = cell / 3f, center = center)
                        drawCircle(Color(0xFFFDF8F2).copy(alpha = 0.35f), radius = cell / 8f, center = center)
                    }
                }
            }
            PatternType.GRADIENT -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = listOf(0f to colors.first(), 1f to colors.getOrElse(1) { colors[0] }),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                    ),
                )
                val band = colors.getOrElse(2) { colors[0] }
                drawRect(band.copy(alpha = 0.45f), topLeft = Offset(0f, h * 0.62f), size = Size(w, h * 0.08f))
                drawRect(band.copy(alpha = 0.30f), topLeft = Offset(0f, h * 0.74f), size = Size(w, h * 0.04f))
            }
            else -> {
                // Kente-style weave: base field, vertical strips, gold weft threads, accent blocks
                val c0 = colors.first()
                val c1 = colors.getOrElse(1) { colors[0] }
                val c2 = colors.getOrElse(2) { BrandGold }
                drawRect(c0)
                val vw = w / 8f
                drawRect(c1, topLeft = Offset(vw, 0f), size = Size(vw, h))
                drawRect(c1, topLeft = Offset(w - 2 * vw, 0f), size = Size(vw, h))
                val thread = h / 12f
                drawRect(BrandGold, topLeft = Offset(0f, h / 3f), size = Size(w, thread))
                drawRect(BrandGold, topLeft = Offset(0f, h * 2f / 3f - thread / 2f), size = Size(w, thread))
                drawRect(Color(0xFFFDF8F2).copy(alpha = 0.8f), topLeft = Offset(0f, h / 3f + thread), size = Size(w, 2f))
                drawRect(Color(0xFFFDF8F2).copy(alpha = 0.8f), topLeft = Offset(0f, h * 2f / 3f + thread / 2f), size = Size(w, 2f))
                drawRect(c2, topLeft = Offset(vw * 1.15f, h / 3f), size = Size(vw * 0.7f, thread))
                drawRect(c2, topLeft = Offset(w - vw * 1.85f, h * 2f / 3f - thread / 2f), size = Size(vw * 0.7f, thread))
            }
        }
        // cloth depth: gentle vignette toward the bottom edge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF000000).copy(alpha = 0.10f)),
                startY = h * 0.55f,
                endY = h,
            ),
        )
        // diagonal sheen
        drawRect(
            brush = Brush.linearGradient(
                colorStops = listOf(0f to Color.White.copy(alpha = 0.14f), 1f to Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(w * 0.7f, h * 0.5f),
            ),
        )
        // hairline frame
        drawRoundRect(
            color = Color(0xFF000000).copy(alpha = 0.10f),
            topLeft = Offset(0.75f, 0.75f),
            size = Size((w - 1.5f).coerceAtLeast(0f), (h - 1.5f).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(w * 0.02f),
            style = Stroke(width = 1.5f),
        )
    }
}

/** Flat garment illustration built from the shared geometry + the analysed palette. */
@Composable
fun GarmentArt(style: DressStyle, fabric: Fabric?, modifier: Modifier = Modifier) {
    val colors = fabric?.paletteColors() ?: FALLBACK
    val shapes = GarmentGeometry.shapesFor(style.silhouette)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // paper plate behind the garment, magazine-cutout style
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFDF8F2), Color(0xFFF0E4D4)),
                startY = h * 0.03f,
                endY = h * 0.97f,
            ),
            topLeft = Offset(w * 0.04f, h * 0.03f),
            size = Size(w * 0.92f, h * 0.94f),
            cornerRadius = CornerRadius(w * 0.05f),
        )
        drawRoundRect(
            color = Color(0xFF14110F).copy(alpha = 0.06f),
            topLeft = Offset(w * 0.04f, h * 0.03f),
            size = Size(w * 0.92f, h * 0.94f),
            cornerRadius = CornerRadius(w * 0.05f),
            style = Stroke(width = 1f),
        )
        // soft shadow under the garment
        drawOval(
            color = Color(0xFF14110F).copy(alpha = 0.10f),
            topLeft = Offset(w * 0.28f, h * 0.88f),
            size = Size(w * 0.44f, h * 0.06f),
        )
        shapes.forEachIndexed { index, poly ->
            val path = Path().apply {
                poly.forEachIndexed { i, (x, y) ->
                    val px = x * w
                    val py = y * h
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            val fill = colors.getOrElse(index % colors.size) { colors[0] }
            drawPath(path, fill)
            drawPath(path, color = fill.darkenForOutline(), style = Stroke(width = w * 0.008f))
            // one accent stripe on the main panel for a designed feel
            if (index == 0) drawStripeAccent(path, colors, w, h)
        }
    }
}

private fun DrawScope.drawStripeAccent(clip: Path, colors: List<Color>, w: Float, h: Float) {
    if (colors.size < 2) return
    // horizontal accent band clipped to the garment
    val band = Path().apply {
        addRect(Rect(w * 0.1f, h * 0.46f, w * 0.9f, h * 0.54f))
    }
    val combined = Path().apply {
        op(clip, band, PathOperation.Intersect)
    }
    drawPath(combined, colors[1 % colors.size], alpha = 0.9f)
}

private fun Color.darkenForOutline(): Color =
    Color(red * 0.55f, green * 0.55f, blue * 0.55f, alpha)

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF14110F), cornerRadius = CornerRadius(w * 0.18f))
        val sheet = Path().apply {
            moveTo(w * 0.28f, h * 0.24f)
            lineTo(w * 0.61f, h * 0.24f)
            lineTo(w * 0.74f, h * 0.37f)
            lineTo(w * 0.74f, h * 0.76f)
            lineTo(w * 0.28f, h * 0.76f)
            close()
        }
        drawPath(sheet, Color(0xFFFDF8F2))
        drawPath(
            Path().apply {
                moveTo(w * 0.61f, h * 0.24f)
                lineTo(w * 0.74f, h * 0.37f)
                lineTo(w * 0.61f, h * 0.37f)
                close()
            },
            Color(0xFFC8952B),
        )
        drawRect(Color(0xFF8C2F39), topLeft = Offset(w * 0.34f, h * 0.48f), size = Size(w * 0.35f, h * 0.07f))
        drawRect(Color(0xFF1F5450), topLeft = Offset(w * 0.34f, h * 0.6f), size = Size(w * 0.24f, h * 0.07f))
    }
}

@Composable
fun SwatchPreview(fabric: Fabric, modifier: Modifier = Modifier) {
    FabricSwatch(fabric = fabric, modifier = modifier.size(56.dp))
}

/**
 * Decodes a bundled catalog photo from assets off the main thread and shows it cropped.
 * Falls back to an empty tonal box while loading or if the asset is missing.
 */
@Composable
fun AssetImage(asset: String, contentDescription: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(asset) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(asset) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open(asset).use { stream ->
                    BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 1 })
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh))
    }
}
