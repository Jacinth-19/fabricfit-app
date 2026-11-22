package com.fabricfit.app.data.repository

import android.content.Context
import com.fabricfit.app.domain.model.AnalyzedColor
import com.fabricfit.app.domain.model.ColorName
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.PatternType
import com.fabricfit.app.domain.model.TextureType
import java.io.File

/**
 * Bundled FitRoom demo inputs so the try-on can be exercised end to end without a
 * photo library or a scanned fabric:
 *
 *  - `model_ama_fullbody.jpg`  — a full-body reference model (Ghanaian woman, plain
 *    clothing, arms clear of the torso) shot against a plain background.
 *  - `dress_kente_online.jpg`  — a Ghanaian kente dress sourced online, kept as the
 *    "bought online" reference garment.
 *  - `fabric_kente_online.jpg` — the cloth swatch cut from that dress, so try-on has a
 *    real kente texture to drape. It is also registered as a scan result so it shows
 *    up in the fabric rail like any other swatch.
 */
object DemoTryOnSamples {

    const val MODEL_ASSET = "catalog/samples/model_ama_fullbody.jpg"
    const val DRESS_ASSET = "catalog/samples/dress_kente_online.jpg"
    const val FABRIC_ASSET = "catalog/samples/fabric_kente_online.jpg"

    const val SOURCE_URL = "https://www.pinterest.com/ideas/ghana-kente-dress-styles/895299763693/"
    const val FABRIC_NAME = "Kente (bought online)"

    /** Copies the bundled model photo into app storage and returns its absolute path. */
    suspend fun personPhoto(context: Context): String = copy(
        context,
        MODEL_ASSET,
        File(context.filesDir, "demo").apply { mkdirs() },
        "ama_fullbody.jpg",
    ).absolutePath

    /** Copies the kente swatch cut from the online dress and returns a `file://` uri. */
    suspend fun fabricSwatch(context: Context): String =
        "file://" + copy(context, FABRIC_ASSET, File(context.filesDir, "demo").apply { mkdirs() }, "fabric_kente_online.jpg").absolutePath

    /**
     * Registers the kente swatch as a fabric scan when it is not already in the closet,
     * so the FitRoom fabric rail (and FabricDetail) can select it. Returns the fabric id.
     */
    suspend fun ensureFabric(context: Context, repo: com.fabricfit.app.domain.repository.FabricAnalysisRepository): Long {
        val existing = kotlinx.coroutines.flow.first(repo.observeAll())
            .firstOrNull { it.name == FABRIC_NAME }
        if (existing != null) return existing.id

        val swatchUri = fabricSwatch(context)
        val colors = samplePalette()
        val fabric = Fabric(
            imageUri = swatchUri,
            name = FABRIC_NAME,
            category = FabricCategory.KENTE,
            colors = colors,
            pattern = PatternType.STRIPED,
            texture = TextureType.WOVEN,
            confidence = 0.93f,
            createdAt = System.currentTimeMillis(),
        )
        return repo.save(fabric)
    }

    private fun samplePalette(): List<AnalyzedColor> = listOf(
        AnalyzedColor(0xFFF2C230L, ColorName.GOLD, 0.34f),
        AnalyzedColor(0xFF2FA7D8L, ColorName.BLUE, 0.28f),
        AnalyzedColor(0xFF2E7D32L, ColorName.GREEN, 0.22f),
        AnalyzedColor(0xFF14110FL, ColorName.BLACK, 0.16f),
    )

    private fun copy(context: Context, asset: String, dir: File, name: String): File {
        val out = File(dir, name)
        if (!out.exists() || out.length() == 0L) {
            context.assets.open(asset).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return out
    }
}
