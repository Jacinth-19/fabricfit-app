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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.media.garment.GarmentGeometry

private val FALLBACK = listOf(Color(0xFFC8952B), Color(0xFF8C2F39), Color(0xFF1F5450))

internal fun Fabric.paletteColors(): List<Color> =
    colors.map { Color(it.argb) }.ifEmpty { FALLBACK }

/**
 * Procedural swatch: renders the analysed palette using the detected pattern family, so every
 * fabric card looks like *that* fabric without bundling photos.
 */
@Composable
fun FabricSwatch(fabric: Fabric, modifier: Modifier = Modifier) {
    val colors = fabric.paletteColors()
    val pattern = fabric.pattern
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(colors.first())
        when (pattern) {
            PatternType.STRIPED -> {
                val bands = (colors.size * 2).coerceAtLeast(4)
                val band = h / bands
                repeat(bands) { i ->
                    drawRect(colors[i % colors.size], topLeft = Offset(0f, i * band), size = Size(w, band))
                }
            }
            PatternType.CHECKED -> {
                val cell = w / 5
                for (cy in 0 until 6) for (cx in 0 until 6) {
                    if ((cx + cy) % 2 == 0) continue
                    drawRect(colors.getOrElse(1) { colors[0] }, topLeft = Offset(cx * cell, cy * cell), size = Size(cell, cell))
                }
            }
            PatternType.DOTTED, PatternType.FLORAL, PatternType.ORGANIC -> {
                val cell = w / 5
                for (cy in 0 until 6) for (cx in 0 until 6) {
                    drawCircle(
                        colors[(cx * 3 + cy) % colors.size],
                        radius = cell / 3f,
                        center = Offset(cx * cell + cell / 2, cy * cell + cell / 2),
                    )
                }
            }
            PatternType.GRADIENT -> {
                drawRect(colors.getOrElse(1) { colors[0] }, topLeft = Offset(0f, h / 2), size = Size(w, h / 2))
            }
            else -> {
                val cell = w / 4
                for (cy in 0 until 5) for (cx in 0 until 5) {
                    drawRect(
                        colors[(cx + cy * 2) % colors.size],
                        topLeft = Offset(cx * cell, cy * cell),
                        size = Size(cell * 0.86f, cell * 0.86f),
                    )
                }
            }
        }
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
            color = Color(0xFFFDF8F2),
            topLeft = Offset(w * 0.04f, h * 0.03f),
            size = Size(w * 0.92f, h * 0.94f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
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
            drawPath(path, color = fill.darkenForOutline(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.008f))
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
        op(clip, band, androidx.compose.ui.graphics.PathOperation.Intersect)
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
        drawRoundRect(Color(0xFF14110F), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f))
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

/**
 * Heritage Luxe adinkra-inspired monogram (from the welcome screen).
 * Symmetrical, woven, royal-gold glyph on transparent background — drop it onto
 * Deep Forest (#0F3D2E) for the splash hero.
 */
@Composable
fun HeritageLuxeMark(modifier: Modifier = Modifier) {
    val gold = Color(0xFFD4A84B)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = w * 0.07f

        // Central disk — the "eye" of the mark
        drawCircle(
            color = gold,
            radius = w * 0.17f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
        drawCircle(
            color = gold,
            radius = w * 0.09f,
            center = Offset(cx, cy),
        )

        // Vertical "spine" running top to bottom
        drawRect(
            color = gold,
            topLeft = Offset(cx - stroke / 2f, h * 0.10f),
            size = Size(stroke, h * 0.80f),
        )

        // Top crown: three prongs
        drawRect(
            color = gold,
            topLeft = Offset(cx - w * 0.20f, h * 0.10f),
            size = Size(stroke, h * 0.12f),
        )
        drawRect(
            color = gold,
            topLeft = Offset(cx + w * 0.20f - stroke, h * 0.10f),
            size = Size(stroke, h * 0.12f),
        )
        // Crown connector arch
        drawArc(
            color = gold,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.20f, h * 0.05f),
            size = Size(w * 0.40f, h * 0.18f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )

        // Mid horizontal band across the circle
        drawRect(
            color = gold,
            topLeft = Offset(w * 0.30f, cy - stroke / 2f),
            size = Size(w * 0.40f, stroke),
        )

        // Lower crown (M-shape): chevrons pointing up and out
        val leftCurl = Path().apply {
            moveTo(cx - w * 0.10f, h * 0.70f)
            cubicTo(
                cx - w * 0.25f, h * 0.72f,
                cx - w * 0.30f, h * 0.85f,
                cx - w * 0.18f, h * 0.90f,
            )
        }
        drawPath(leftCurl, gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
        val rightCurl = Path().apply {
            moveTo(cx + w * 0.10f, h * 0.70f)
            cubicTo(
                cx + w * 0.25f, h * 0.72f,
                cx + w * 0.30f, h * 0.85f,
                cx + w * 0.18f, h * 0.90f,
            )
        }
        drawPath(rightCurl, gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))

        // Left + right curved "wings" flanking the disk
        val leftWing = Path().apply {
            moveTo(w * 0.16f, h * 0.25f)
            cubicTo(
                w * 0.08f, h * 0.40f,
                w * 0.08f, h * 0.60f,
                w * 0.16f, h * 0.75f,
            )
        }
        drawPath(leftWing, gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
        val rightWing = Path().apply {
            moveTo(w * 0.84f, h * 0.25f)
            cubicTo(
                w * 0.92f, h * 0.40f,
                w * 0.92f, h * 0.60f,
                w * 0.84f, h * 0.75f,
            )
        }
        drawPath(rightWing, gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
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
