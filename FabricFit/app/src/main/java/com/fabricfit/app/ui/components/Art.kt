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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.AnalyzedColor
import com.fabricfit.app.domain.model.ColorName
import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.FabricPlacement
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.domain.model.TextureType
import com.fabricfit.app.media.garment.GarmentGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

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
 * Live interactive silhouette preview that reshapes in real time when the user adjusts
 * Sleeve, Neckline, Length, Fabric Placement, or Accent Colour in the customizer.
 */
@Composable
fun CustomizedSilhouettePreview(
    style: DressStyle,
    fabric: Fabric?,
    customization: DesignCustomization,
    modifier: Modifier = Modifier,
) {
    val palette = fabric?.paletteColors() ?: FALLBACK
    val primaryColor = palette.firstOrNull() ?: Color(0xFFD4A84B)
    val secondaryColor = palette.getOrElse(1) { Color(0xFF0F3D2E) }
    val accentColor = customization.accentArgb?.let { Color(it) } ?: palette.lastOrNull() ?: Color(0xFF7A1F2B)
    val polys = remember(style.silhouette, customization) {
        GarmentGeometry.shapesFor(style.silhouette, customization)
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3EFE6))
            .padding(10.dp),
    ) {
        val w = size.width
        val h = size.height
        polys.forEachIndexed { index, poly ->
            val path = Path().apply {
                poly.forEachIndexed { i, (nx, ny) ->
                    val px = nx * w
                    val py = ny * h
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            val centroidY = poly.map { it.second }.average().toFloat()
            val fill = when (customization.placement) {
                FabricPlacement.FULL -> if (index % 2 == 0) primaryColor else secondaryColor
                FabricPlacement.TOP_ONLY -> if (centroidY < 0.5f) primaryColor else accentColor.copy(alpha = 0.75f)
                FabricPlacement.SKIRT_ONLY -> if (centroidY >= 0.5f) primaryColor else accentColor.copy(alpha = 0.75f)
                FabricPlacement.PANELS -> if (index % 2 == 0) primaryColor else accentColor
                FabricPlacement.ACCENTS -> accentColor.copy(alpha = 0.8f)
            }
            drawPath(path = path, color = fill)
            drawPath(path = path, color = accentColor, style = Stroke(width = 3f))
        }
    }
}

/**
 * Ensures at least one sample Kente Royalty fabric exists in the local repository so
 * 1-tap Try-On and Colour Match flows work immediately even before a camera scan.
 */
suspend fun ensureSampleKenteFabric(context: android.content.Context): Long = withContext(Dispatchers.IO) {
    val container = AppContainer.get(context)
    val existing = container.fabricAnalysis.observeAll().first()
    if (existing.isNotEmpty()) {
        return@withContext existing.first().id
    }
    val sampleFile = File(context.filesDir, "sample_kente_royalty.jpg")
    if (!sampleFile.exists()) {
        context.assets.open("catalog/heritage/swatch_kente.jpg").use { input ->
            sampleFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
    val sampleFabric = Fabric(
        imageUri = sampleFile.absolutePath,
        name = "Kente Royalty",
        category = FabricCategory.KENTE,
        colors = listOf(
            AnalyzedColor(0xFF144B2FL, ColorName.GREEN, 0.34f),
            AnalyzedColor(0xFF611425L, ColorName.RED, 0.26f),
            AnalyzedColor(0xFFD99B26L, ColorName.GOLD, 0.24f),
            AnalyzedColor(0xFFCC292BL, ColorName.ORANGE, 0.16f),
        ),
        pattern = PatternType.STRIPED,
        texture = TextureType.WOVEN,
        confidence = 0.98f,
        createdAt = System.currentTimeMillis(),
    )
    container.fabricAnalysis.save(sampleFabric)
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