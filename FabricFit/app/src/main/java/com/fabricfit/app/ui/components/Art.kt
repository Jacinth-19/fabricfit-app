package com.fabricfit.app.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.PatternType

private val FALLBACK = listOf(Color(0xFFD4A84B), Color(0xFF0F3D2E), Color(0xFF7A1F2B))

private val assetBitmapCache = object : LruCache<String, android.graphics.Bitmap>(48) {}

internal fun Fabric.paletteColors(): List<Color> =
    colors.map { Color(it.argb) }.ifEmpty { FALLBACK }

/**
 * Maps each catalogue [DressStyle] to a bundled Heritage Luxe Ghanaian fashion asset
 * so cards across Home, Discover, FitMatch AI, and Favourites render rich editorial
 * imagery instead of flat geometric placeholders.
 */
fun DressStyle.heritageAsset(): String = when (id) {
    "kaba_slit" -> "catalog/heritage/outfit_modern_kente.jpg"
    "modern_kaba" -> "catalog/heritage/outfit_elegant_peplum.jpg"
    "maxi_dress" -> "catalog/heritage/outfit_maxi_dress.jpg"
    "co_ords" -> "catalog/heritage/outfit_twopiece_set.jpg"
    "peplum" -> "catalog/heritage/dress_kente_asymmetric.jpg"
    "african_gown" -> "catalog/heritage/dress_kente_flare.jpg"
    "straight_dress" -> "catalog/heritage/dress_kente_aline.jpg"
    "mermaid_dress" -> "catalog/looks/look_lace_women_1.jpg"
    "skirt_blouse" -> "catalog/looks/look_kente_women_1.jpg"
    "jumpsuit" -> "catalog/looks/look_ankara_women_2.jpg"
    "contemporary_formal" -> "catalog/looks/look_lace_women_2.jpg"
    "kaftan" -> "catalog/looks/look_kente_men_1.jpg"
    "agbada" -> "catalog/looks/look_kente_men_2.jpg"
    "print_shirt" -> "catalog/looks/look_ankara_men_1.jpg"
    "senator" -> "catalog/looks/look_ankara_men_2.jpg"
    "smock" -> "catalog/looks/look_smock_men_1.jpg"
    "african_suit" -> "catalog/looks/look_kente_men_1.jpg"
    "shirt_trousers" -> "catalog/looks/look_ankara_men_2.jpg"
    "modern_traditional" -> "catalog/looks/look_smock_men_2.jpg"
    "cover_up" -> "catalog/looks/look_ankara_women_1.jpg"
    else -> "catalog/heritage/outfit_modern_kente.jpg"
}

fun DressStyle.heritagePrice(): String = when (id) {
    "kaba_slit" -> "$78.00"
    "modern_kaba" -> "$89.00"
    "maxi_dress" -> "$95.00"
    "co_ords" -> "$110.00"
    "peplum" -> "$89.00"
    "african_gown" -> "$125.00"
    "straight_dress" -> "$82.00"
    "mermaid_dress" -> "$130.00"
    "skirt_blouse" -> "$85.00"
    "jumpsuit" -> "$92.00"
    "contemporary_formal" -> "$98.00"
    "kaftan" -> "$95.00"
    "agbada" -> "$140.00"
    "print_shirt" -> "$65.00"
    "senator" -> "$110.00"
    "smock" -> "$105.00"
    "african_suit" -> "$120.00"
    "shirt_trousers" -> "$88.00"
    "modern_traditional" -> "$99.00"
    else -> "$89.00"
}

fun DressStyle.heritageRating(): String = when (id) {
    "kaba_slit" -> "4.7"
    "modern_kaba" -> "4.8"
    "maxi_dress" -> "4.6"
    "co_ords" -> "4.9"
    "peplum" -> "4.8"
    "african_gown" -> "4.9"
    "straight_dress" -> "4.8"
    "mermaid_dress" -> "4.9"
    "smock" -> "4.9"
    "agbada" -> "4.9"
    else -> "4.8"
}

fun DressStyle.heritageReviews(): Int = when (id) {
    "kaba_slit" -> 124
    "modern_kaba" -> 122
    "maxi_dress" -> 98
    "co_ords" -> 146
    "peplum" -> 79
    "african_gown" -> 164
    "straight_dress" -> 88
    else -> 112
}

/**
 * Procedural swatch: renders the analysed palette using the detected pattern family, so every
 * fabric card looks like *that* fabric without bundling photos.
 */
@Composable
fun FabricSwatch(fabric: Fabric, modifier: Modifier = Modifier) {
    val colors = fabric.paletteColors()
    val pattern = fabric.pattern
    val asset = fabric.category.asset
    if (asset != null && fabric.imageUri.isBlank()) {
        AssetImage(
            asset = asset,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
        )
        return
    }
    Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
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

/**
 * Editorial garment artwork: renders the bundled Heritage Luxe Ghanaian look for [style],
 * with a fabric swatch badge when paired with a specific scanned [fabric].
 */
@Composable
fun GarmentArt(style: DressStyle, fabric: Fabric?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp))) {
        AssetImage(
            asset = style.heritageAsset(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        if (fabric != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.5.dp, Color(0xFFD4A84B), RoundedCornerShape(6.dp)),
            ) {
                FabricSwatch(fabric, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    HeritageLuxeMark(modifier = modifier)
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
 * Decodes a bundled catalog photo from assets with an in-memory LRU cache so that
 * first-frame composition and emulator screenshot captures render synchronously.
 */
@Composable
fun AssetImage(
    asset: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val bmp = remember(asset) {
        synchronized(assetBitmapCache) {
            assetBitmapCache.get(asset)
        } ?: try {
            context.assets.open(asset).use { stream ->
                BitmapFactory.decodeStream(stream)?.also { decoded ->
                    synchronized(assetBitmapCache) {
                        assetBitmapCache.put(asset, decoded)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh))
    }
}
