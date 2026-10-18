package com.fabricfit.app.ui.components

import android.content.Context
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.media.garment.GarmentGeometry
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.AnalyzedColor
import com.fabricfit.app.domain.model.ColorName
import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.FabricPlacement
import com.fabricfit.app.domain.model.TextureType
import com.fabricfit.app.ui.util.resolveKey
import java.io.File
import kotlinx.coroutines.flow.first

private val FALLBACK = listOf(Color(0xFFD4A84B), Color(0xFF0F3D2E), Color(0xFF7A1F2B))

private val assetBitmapCache = object : LruCache<String, android.graphics.Bitmap>(48) {}

internal fun Fabric.paletteColors(): List<Color> =
    colors.map { Color(it.argb) }.ifEmpty { FALLBACK }

fun DressStyle.heritageTitle(context: Context): String = when (id) {
    "kaba_slit" -> "Modern Kente Dress"
    "modern_kaba" -> "Elegant Peplum Dress"
    "maxi_dress" -> "Maxi Dress"
    "co_ords" -> "Two-Piece Set"
    "african_gown" -> "Flare Kente Gown"
    "straight_dress" -> "Royal Kente Gown"
    "peplum" -> "Asymmetric Peplum"
    else -> context.resolveKey(titleKey) ?: id
}

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
    "peplum" -> "$59.00"
    "african_gown" -> "$89.00"
    "straight_dress" -> "$89.00"
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
    AssetImage(
        asset = "catalog/heritage/monogram_gold.png",
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
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

@Composable
fun CustomizedSilhouettePreview(
    style: DressStyle,
    fabric: Fabric?,
    customization: DesignCustomization,
    modifier: Modifier = Modifier,
) {
    val palette = fabric?.paletteColors() ?: FALLBACK
    val shapes = remember(style.silhouette, customization) {
        GarmentGeometry.shapesFor(style.silhouette, customization)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3EFE6)),
        contentAlignment = Alignment.Center,
    ) {
        AssetImage(
            asset = style.heritageAsset(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xBB0F3D2E)),
        )
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height
            val primaryColor = palette.first()
            val accentColor = palette.getOrElse(1) { Color(0xFFD4A84B) }

            shapes.forEachIndexed { idx, polygon ->
                if (polygon.size >= 3) {
                    val path = Path().apply {
                        moveTo(polygon[0].first * w, polygon[0].second * h)
                        for (i in 1 until polygon.size) {
                            lineTo(polygon[i].first * w, polygon[i].second * h)
                        }
                        close()
                    }
                    val fillColor = when (customization.placement) {
                        FabricPlacement.FULL -> if (idx == 0) primaryColor.copy(alpha = 0.78f) else accentColor.copy(alpha = 0.72f)
                        FabricPlacement.TOP_ONLY -> if (idx == 0) primaryColor.copy(alpha = 0.82f) else Color(0xFFFBF8F1).copy(alpha = 0.55f)
                        FabricPlacement.SKIRT_ONLY -> if (idx == 0) Color(0xFFFBF8F1).copy(alpha = 0.55f) else primaryColor.copy(alpha = 0.82f)
                        FabricPlacement.ACCENTS -> Color(0xFFFBF8F1).copy(alpha = 0.45f)
                        FabricPlacement.PANELS -> if (idx % 2 == 0) primaryColor.copy(alpha = 0.75f) else accentColor.copy(alpha = 0.65f)
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(
                        path = path,
                        color = Color(0xFFD4A84B),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
    }
}

suspend fun ensureSampleKenteFabric(context: Context): Long {
    val repo = AppContainer.get(context).fabricAnalysis
    val existing = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val current = repo.observeAll().first()
        current.firstOrNull { it.category == FabricCategory.KENTE } ?: current.firstOrNull()
    }
    if (existing != null) return existing.id

    val uri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        SampleFabrics.materialize(context, SampleFabrics.all.first()) ?: run {
            val dir = File(context.filesDir, "media").apply { mkdirs() }
            val out = File(dir, "sample_kente.jpg")
            if (!out.exists()) {
                context.assets.open("catalog/heritage/swatch_kente.jpg").use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
            android.net.Uri.fromFile(out).toString()
        }
    }

    val sample = Fabric(
        imageUri = uri,
        name = "Royal Ashanti Kente",
        category = FabricCategory.KENTE,
        colors = listOf(
            AnalyzedColor(0xFFD4A84BL, ColorName.GOLD, 0.38f),
            AnalyzedColor(0xFF0F3D2EL, ColorName.GREEN, 0.32f),
            AnalyzedColor(0xFF8C2F39L, ColorName.RED, 0.18f),
            AnalyzedColor(0xFF14110FL, ColorName.BLACK, 0.12f),
        ),
        pattern = PatternType.STRIPED,
        texture = TextureType.WOVEN,
        confidence = 0.94f,
        createdAt = System.currentTimeMillis(),
    )
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        repo.save(sample)
    }
}
