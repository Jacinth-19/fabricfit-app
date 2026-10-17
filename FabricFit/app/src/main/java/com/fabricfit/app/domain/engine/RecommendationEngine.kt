package com.fabricfit.app.domain.engine

import android.content.Context
import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.Fabric
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.GarmentSilhouette
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.Measurements
import com.fabricfit.app.domain.model.Recommendation
import com.fabricfit.app.domain.model.StyleCategory
import com.fabricfit.app.domain.model.TextureType
import com.fabricfit.app.domain.model.WhyReason
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.abs

/**
 * FitMatch Context-Aware Recommendation Ranker + Neural TFLite Ranker (`fitmatch_ranker.tflite`):
 * Combines fabric structure, motif scale, Gabor weave anisotropy, wearer skin-undertone colour
 * harmony, tailoring body measurements, and session style affinity into a calibrated 0..98 match
 * score with explainable [WhyReason] tags.
 */
object RecommendationEngine {

    private const val FITMATCH_MODEL_ASSET = "fitmatch_ranker.tflite"
    const val FEATURE_DIM = 16

    @Volatile
    private var rankerInterpreter: Interpreter? = null

    private fun interpreterOrNull(appContext: Context?): Interpreter? {
        if (appContext == null) return null
        rankerInterpreter?.let { return it }
        return runCatching {
            val afd = appContext.assets.openFd(FITMATCH_MODEL_ASSET)
            val buf = FileInputStream(afd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength,
            )
            Interpreter(buf, Interpreter.Options().apply { setNumThreads(1) }).also {
                rankerInterpreter = it
            }
        }.getOrNull()
    }

    /**
     * Optional personalization & biometric context for context-aware FitMatch ranking.
     */
    data class RecommendationContext(
        val skinToneArgb: Long? = null,
        val measurements: Measurements? = null,
        val weaveAnisotropy: Float? = null,
        val favoriteStyleIds: Set<String> = emptySet(),
        val preferredGender: GenderCategory? = null,
        val appContext: Context? = null,
    )

    private val LARGE_PANEL = setOf(
        GarmentSilhouette.KABA, GarmentSilhouette.MODERN_KABA, GarmentSilhouette.MAXI,
        GarmentSilhouette.GOWN, GarmentSilhouette.AGBADA, GarmentSilhouette.KAFTAN,
        GarmentSilhouette.SMOCK, GarmentSilhouette.COVER_UP,
    )

    private val FITTED = setOf(
        GarmentSilhouette.STRAIGHT, GarmentSilhouette.MERMAID, GarmentSilhouette.PEPLUM,
        GarmentSilhouette.PRINT_SHIRT, GarmentSilhouette.SENATOR,
        GarmentSilhouette.SHIRT_TROUSERS, GarmentSilhouette.FORMAL_DRESS,
        GarmentSilhouette.MODERN_TRADITIONAL,
    )

    private val WAIST_DEFINED_SILHOUETTES = setOf(
        GarmentSilhouette.MERMAID, GarmentSilhouette.PEPLUM,
        GarmentSilhouette.KABA, GarmentSilhouette.MODERN_KABA,
        GarmentSilhouette.SKIRT_BLOUSE, GarmentSilhouette.FORMAL_DRESS,
    )

    private val TALL_COMMANDING_SILHOUETTES = setOf(
        GarmentSilhouette.AGBADA, GarmentSilhouette.KAFTAN, GarmentSilhouette.SMOCK,
        GarmentSilhouette.MAXI, GarmentSilhouette.GOWN, GarmentSilhouette.SENATOR,
    )

    private val ELONGATING_VERTICAL_SILHOUETTES = setOf(
        GarmentSilhouette.STRAIGHT, GarmentSilhouette.MODERN_TRADITIONAL,
        GarmentSilhouette.JUMPSUIT, GarmentSilhouette.CO_ORDS, GarmentSilhouette.SENATOR,
    )

    private val PANEL_ALIGNED_SILHOUETTES = setOf(
        GarmentSilhouette.KABA, GarmentSilhouette.MODERN_KABA, GarmentSilhouette.AGBADA,
        GarmentSilhouette.SMOCK, GarmentSilhouette.SENATOR, GarmentSilhouette.STRAIGHT,
    )

    private val STRUCTURED_PATTERNS = setOf(
        com.fabricfit.app.domain.model.PatternType.STRIPED,
        com.fabricfit.app.domain.model.PatternType.CHECKED,
        com.fabricfit.app.domain.model.PatternType.GEOMETRIC,
        com.fabricfit.app.domain.model.PatternType.SYMBOLIC,
    )

    private fun textureStructure(t: TextureType): Float = when (t) {
        TextureType.WOVEN -> 0.7f
        TextureType.STIFF -> 0.9f
        TextureType.SLUB -> 0.6f
        TextureType.SMOOTH -> 0.4f
        TextureType.SOFT -> 0.2f
        TextureType.SHEER -> 0.15f
        TextureType.PILE -> 0.8f
        TextureType.QUILTED -> 0.9f
        TextureType.EMBROIDERED -> 0.75f
    }

    /**
     * Builds the 16-dimensional normalized feature vector consumed by `fitmatch_ranker.tflite`.
     */
    fun buildFeatureVector(
        fabric: Fabric,
        style: DressStyle,
        context: RecommendationContext = RecommendationContext(),
        favoriteCategories: Set<StyleCategory> = emptySet(),
    ): FloatArray {
        val targetStructure = textureStructure(fabric.texture)
        val overlap = fabric.suggestedUses.intersect(style.occasions).size.toFloat() /
            style.occasions.size.coerceAtLeast(1).toFloat()
        val skinHarmony = context.skinToneArgb?.let {
            ColorHarmonyEngine.skinHarmonyScore(fabric.colors.map { c -> c.argb }, it)
        } ?: 0.5f
        val anisotropy = (context.weaveAnisotropy ?: 0f).coerceIn(0f, 1f)
        val m = context.measurements
        val whrFit = if (m?.waistCm != null && m.hipCm != null && m.hipCm > 0) {
            val whr = m.waistCm.toFloat() / m.hipCm.toFloat()
            when {
                whr < 0.78f && style.silhouette in WAIST_DEFINED_SILHOUETTES -> 1f
                whr >= 0.85f && style.silhouette in LARGE_PANEL -> 0.8f
                else -> 0.3f
            }
        } else 0f
        val heightFit = if (m?.heightCm != null) {
            when {
                m.heightCm >= 175 && style.silhouette in TALL_COMMANDING_SILHOUETTES -> 1f
                m.heightCm in 140..162 && style.silhouette in ELONGATING_VERTICAL_SILHOUETTES -> 1f
                else -> 0.25f
            }
        } else 0f
        val favAffinity = when {
            style.id in context.favoriteStyleIds -> 1f
            style.category in favoriteCategories -> 0.5f
            else -> 0f
        }
        val genderMatch = if (context.preferredGender != null && scoreForGender(style, context.preferredGender)) 1f else 0f

        return floatArrayOf(
            targetStructure,
            style.structure.coerceIn(0f, 1f),
            fabric.motifScale.coerceIn(0f, 1f),
            if (style.silhouette in LARGE_PANEL) 1f else 0f,
            if (style.silhouette in FITTED) 1f else 0f,
            if (fabric.category in style.affinity) 1f else 0f,
            overlap.coerceIn(0f, 1f),
            fabric.palette.saturation.coerceIn(0f, 1f),
            if (fabric.category in TRADITIONAL_CLOTHS && style.category == StyleCategory.TRADITIONAL) 1f else 0f,
            skinHarmony.coerceIn(0f, 1f),
            anisotropy,
            if (style.silhouette in PANEL_ALIGNED_SILHOUETTES) 1f else 0f,
            whrFit,
            heightFit,
            favAffinity,
            genderMatch,
        )
    }

    fun recommend(
        fabric: Fabric,
        styles: List<DressStyle>,
        dismissed: Set<String> = emptySet(),
        context: RecommendationContext = RecommendationContext(),
    ): List<Recommendation> {
        val visible = styles.filterNot { it.id in dismissed }
        val targetStructure = textureStructure(fabric.texture)
        val structured = fabric.pattern in STRUCTURED_PATTERNS
        val fabricColorArgbs = fabric.colors.map { it.argb }
        val skinHarmony = context.skinToneArgb?.let {
            ColorHarmonyEngine.skinHarmonyScore(fabricColorArgbs, it)
        }
        val favoriteCategories = styles
            .filter { it.id in context.favoriteStyleIds }
            .map { it.category }
            .toSet()
        val tflite = interpreterOrNull(context.appContext)

        return visible.map { style ->
            var score = 30f
            val reasons = mutableListOf<WhyReason>()

            if (fabric.category in style.affinity) {
                score += 18f
            }

            val structureFit = 1f - abs(targetStructure - style.structure)
            score += 10f * structureFit
            if (structureFit > 0.75f) {
                reasons += if (targetStructure > 0.55f) WhyReason.TextureHeavy else WhyReason.TextureLight
            }

            if (structured) {
                reasons += WhyReason.PatternStructured
                score += 6f
            } else {
                reasons += WhyReason.PatternFlowing
                score += 4f
            }

            when {
                fabric.motifScale > 0.55f && style.silhouette in LARGE_PANEL -> {
                    score += 8f
                    reasons += WhyReason.ScaleLarge
                }
                fabric.motifScale < 0.35f && style.silhouette in FITTED -> {
                    score += 8f
                    reasons += WhyReason.ScaleSmall
                }
            }

            val overlap = fabric.suggestedUses.intersect(style.occasions)
            if (overlap.isNotEmpty()) {
                score += 12f * overlap.size / style.occasions.size.coerceAtLeast(1)
                reasons += WhyReason.OccasionFit(overlap.first())
            }

            if (fabric.palette.saturation > 0.5f) {
                score += 6f
                reasons += WhyReason.ColorsRich
            } else if (fabric.palette.saturation < 0.3f) {
                score += 6f
                reasons += WhyReason.ColorsMuted
            }

            if (fabric.category in TRADITIONAL_CLOTHS && style.category == StyleCategory.TRADITIONAL) {
                score += 8f
                reasons += WhyReason.Tradition
            }

            // Context Signal 1: Wearer skin-tone & undertone colour harmony
            if (skinHarmony != null) {
                score += (skinHarmony - 0.5f) * 10f
            }

            // Context Signal 2: Gabor weave anisotropy -> panel-aligned silhouettes
            val anisotropy = context.weaveAnisotropy ?: 0f
            if (anisotropy >= 0.35f && style.silhouette in PANEL_ALIGNED_SILHOUETTES) {
                score += 5f * anisotropy.coerceIn(0f, 1f)
            }

            // Context Signal 3: Tailoring body measurements proportion fit
            context.measurements?.let { m ->
                score += measurementFitBonus(m, style)
            }

            // Context Signal 4: Session favorite affinity & preferred gender
            if (style.id in context.favoriteStyleIds) {
                score += 5f
            } else if (style.category in favoriteCategories) {
                score += 2.5f
            }
            if (context.preferredGender != null && scoreForGender(style, context.preferredGender)) {
                score += 3f
            }

            // Optional Neural TFLite Ranker fusion (`fitmatch_ranker.tflite`)
            if (tflite != null) {
                val features = buildFeatureVector(fabric, style, context, favoriteCategories)
                val neuralProb = runCatching {
                    val inBuf = ByteBuffer.allocateDirect(4 * FEATURE_DIM).order(ByteOrder.nativeOrder())
                    for (f in features) inBuf.putFloat(f)
                    inBuf.rewind()
                    val out = Array(1) { FloatArray(1) }
                    synchronized(tflite) { tflite.run(inBuf, out) }
                    out[0][0]
                }.getOrNull()
                if (neuralProb != null) {
                    score = score * 0.65f + (neuralProb * 100f) * 0.35f
                }
            }

            reasons += WhyReason.GenderFit(style.gender)

            Recommendation(
                fabricId = fabric.id.takeIf { it != 0L },
                style = style,
                score = score.toInt().coerceIn(5, 98),
                reasons = reasons.distinct().take(4),
            )
        }
            .sortedByDescending { it.score }
    }

    /**
     * Evaluates how well a [style]'s silhouette flatters the user's [Measurements] proportions.
     */
    internal fun measurementFitBonus(m: Measurements, style: DressStyle): Float {
        if (m.isEmpty) return 0f
        var bonus = 0f
        val waist = m.waistCm
        val hip = m.hipCm
        if (waist != null && hip != null && hip > 0) {
            val whr = waist.toFloat() / hip.toFloat()
            if (whr < 0.78f && style.silhouette in WAIST_DEFINED_SILHOUETTES) {
                bonus += 4.5f
            } else if (whr >= 0.85f && style.silhouette in LARGE_PANEL) {
                bonus += 3.5f
            }
        }
        val height = m.heightCm
        if (height != null) {
            if (height >= 175 && style.silhouette in TALL_COMMANDING_SILHOUETTES) {
                bonus += 3.5f
            } else if (height in 140..162 && style.silhouette in ELONGATING_VERTICAL_SILHOUETTES) {
                bonus += 3.5f
            }
        }
        return bonus
    }

    /** Convenience for Discover (no fabric selected). */
    fun scoreForGender(style: DressStyle, gender: GenderCategory?): Boolean =
        gender == null || style.gender == gender || style.gender == GenderCategory.UNISEX

    private val TRADITIONAL_CLOTHS = setOf(
        FabricCategory.KENTE, FabricCategory.ADINKRA, FabricCategory.KENTE_PRINT, FabricCategory.WAX,
        FabricCategory.FUGU, FabricCategory.GONJA,
    )
}
