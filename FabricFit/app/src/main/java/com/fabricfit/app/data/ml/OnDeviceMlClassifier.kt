package com.fabricfit.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.MlHint
import com.fabricfit.app.media.ColorPatternAnalyzer.FabricSignature
import com.fabricfit.app.media.ImageProcessor
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.ln

/**
 * Dual-stage on-device TFLite textile classifier + neural weave extractor:
 *
 *  1. **Dedicated 17-Class Ghanaian Fabric Classifier (`ghanaian_fabric_classifier.tflite`)**:
 *     128x128 RGB float32 input -> 17-class softmax output over `ghanaian_fabric_labels.txt`
 *     (`ADINKRA`, `ANKARA`, `BATIK`, `BROCADE`, `CHIFFON`, `COTTON_PLAIN`, `DENIM`, `FUGU`,
 *     `GONJA`, `KENTE`, `KENTE_PRINT`, `LACE`, `LINEN`, `SILK`, `TIEDYE`, `VELVET`, `WAX`).
 *  2. **Out-of-Distribution (OOD) Gate (`mobilenet_v2_1.0_224_quant.tflite`)**:
 *     224x224 quantized MobileNetV2 over ImageNet-1k (`imagenet_labels.txt`) with calibrated
 *     Shannon entropy and top-1/top-2 margin gating to reject non-fabric images.
 *  3. **Neural Weave & Motif Extractor (`weave_motif_extractor.tflite`)**:
 *     128x128 RGB float32 input -> 6-element vector
 *     `[energy_0deg, energy_45deg, energy_90deg, energy_135deg, strip_width_norm, motif_repeat_norm]`.
 */
class OnDeviceMlClassifier(
    private val context: Context,
    private val imageProcessor: ImageProcessor,
) {

    data class ClassifierVerdict(
        val hints: List<MlHint>,
        val predictedCategory: FabricCategory?,
        val calibratedConfidence: Float,
        val isOutOfDistribution: Boolean,
        val uncertaintyReason: UncertaintyReason = UncertaintyReason.NONE,
        val neuralWeaveMetrics: FloatArray? = null,
    )

    enum class UncertaintyReason {
        NONE,
        CONFIDENT_TEXTILE,
        LOW_TOP1_CONFIDENCE,
        HIGH_ENTROPY,
        SMALL_TOP1_TOP2_MARGIN,
        NON_TEXTILE_DOMINANT,
    }

    private val ghanaianLabels: List<String> by lazy {
        runCatching {
            context.assets.open(GHANAIAN_LABELS_ASSET).bufferedReader().useLines { it.toList() }
        }.getOrDefault(emptyList())
    }

    private val imagenetLabels: List<String> by lazy {
        runCatching {
            context.assets.open(IMAGENET_LABELS_ASSET).bufferedReader().useLines { it.toList() }
        }.getOrDefault(emptyList())
    }

    private val ghanaianInterpreter: Interpreter? by lazy {
        loadAssetInterpreter(GHANAIAN_MODEL_ASSET)
    }

    private val oodInterpreter: Interpreter? by lazy {
        loadAssetInterpreter(IMAGENET_MODEL_ASSET)
    }

    private val weaveInterpreter: Interpreter? by lazy {
        loadAssetInterpreter(WEAVE_MODEL_ASSET)
    }

    private fun loadAssetInterpreter(assetName: String): Interpreter? =
        runCatching {
            val afd = context.assets.openFd(assetName)
            val mapped: MappedByteBuffer = FileInputStream(afd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength,
            )
            Interpreter(mapped, Interpreter.Options().apply { setNumThreads(2) })
        }.getOrNull()

    /** Top-3 ImageNet/textile predictions; empty when the model is absent or inference fails. */
    fun topHints(uri: Uri): List<MlHint> {
        val loaded = imageProcessor.load(uri, maxDimension = 320)
        val bmp = (loaded as? com.fabricfit.app.domain.model.Outcome.Success)?.data ?: return emptyList()
        return try {
            val ghanaianScores = runGhanaianClassifier(bmp)
            if (ghanaianScores != null && ghanaianLabels.size == ghanaianScores.size) {
                ghanaianScores.indices
                    .sortedByDescending { ghanaianScores[it] }
                    .take(3)
                    .map { i ->
                        MlHint(
                            label = ghanaianLabels[i].lowercase(),
                            confidence = ghanaianScores[i].coerceIn(0f, 1f),
                        )
                    }
            } else {
                val scores = runImagenetOod(bmp) ?: return emptyList()
                scores.indices
                    .drop(1)
                    .sortedByDescending { scores[it] }
                    .take(3)
                    .mapNotNull { i ->
                        val label = imagenetLabels.getOrNull(i)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        MlHint(label = label, confidence = scores[i].coerceIn(0f, 1f))
                    }
            }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    /**
     * Runs the 17-class Ghanaian textile TFLite model (`ghanaian_fabric_classifier.tflite`),
     * the ImageNet OOD gate (`mobilenet_v2_1.0_224_quant.tflite`), and the neural weave extractor
     * (`weave_motif_extractor.tflite`), fusing them with the multi-scale Gabor/spectral
     * [FabricSignature].
     */
    fun classifyWithUncertainty(uri: Uri, signature: FabricSignature? = null): ClassifierVerdict {
        val loaded = imageProcessor.load(uri, maxDimension = 320)
        val bmp = (loaded as? com.fabricfit.app.domain.model.Outcome.Success)?.data
            ?: return ClassifierVerdict(
                hints = emptyList(),
                predictedCategory = signature?.category,
                calibratedConfidence = signature?.categoryConfidence ?: 0f,
                isOutOfDistribution = true,
                uncertaintyReason = UncertaintyReason.LOW_TOP1_CONFIDENCE,
            )
        return try {
            val oodScores = runImagenetOod(bmp)
            val oodEval = if (oodScores != null) {
                evaluateUncertainty(oodScores, imagenetLabels)
            } else {
                null
            }

            val ghanaianScores = runGhanaianClassifier(bmp)
            val weaveMetrics = runWeaveExtractor(bmp)

            val hints = if (ghanaianScores != null && ghanaianLabels.size == ghanaianScores.size) {
                ghanaianScores.indices
                    .sortedByDescending { ghanaianScores[it] }
                    .take(3)
                    .map { i ->
                        MlHint(
                            label = ghanaianLabels[i].lowercase(),
                            confidence = ghanaianScores[i].coerceIn(0f, 1f),
                        )
                    }
            } else {
                oodEval?.hints ?: emptyList()
            }

            // If ImageNet OOD gate strongly flags a non-textile object AND spectral weave energy is weak, reject
            val strongNonTextileOod = oodEval?.reason == UncertaintyReason.NON_TEXTILE_DOMINANT &&
                (signature == null || signature.gabor.anisotropy < 0.22f)

            val topGhanaianIdx = ghanaianScores?.indices?.maxByOrNull { ghanaianScores[it] }
            val topGhanaianCat = topGhanaianIdx
                ?.let { ghanaianLabels.getOrNull(it) }
                ?.let { runCatching { FabricCategory.valueOf(it.trim().uppercase()) }.getOrNull() }
            val topGhanaianProb = topGhanaianIdx?.let { ghanaianScores[it] } ?: 0f

            val fusedCategory = when {
                strongNonTextileOod -> signature?.category ?: FabricCategory.UNKNOWN
                oodEval != null && oodEval.isConfidentTextile && oodEval.category != null -> oodEval.category
                topGhanaianCat != null && topGhanaianProb >= 0.38f -> {
                    // Preserve high-confidence spectral/strip detections when Gabor anisotropy is very high
                    if (signature != null && signature.gabor.anisotropy > 0.45f &&
                        signature.category in setOf(FabricCategory.KENTE, FabricCategory.FUGU, FabricCategory.GONJA)
                    ) {
                        signature.category
                    } else {
                        topGhanaianCat
                    }
                }
                else -> signature?.category
            }

            val fusedConfidence = when {
                strongNonTextileOod -> (signature?.categoryConfidence ?: 0.35f).coerceAtMost(0.42f)
                topGhanaianCat != null && topGhanaianProb >= 0.38f -> {
                    val sigConf = signature?.categoryConfidence ?: 0.55f
                    (topGhanaianProb * 0.50f + sigConf * 0.50f).coerceIn(0.35f, 0.80f)
                }
                oodEval != null && oodEval.isConfidentTextile -> {
                    val sigConf = signature?.categoryConfidence ?: 0.55f
                    (oodEval.calibratedConfidence * 0.55f + sigConf * 0.45f).coerceIn(0.35f, 0.80f)
                }
                else -> (signature?.categoryConfidence ?: 0.35f).coerceIn(0.30f, 0.72f)
            }

            ClassifierVerdict(
                hints = hints,
                predictedCategory = fusedCategory,
                calibratedConfidence = fusedConfidence,
                isOutOfDistribution = strongNonTextileOod,
                uncertaintyReason = oodEval?.reason ?: UncertaintyReason.CONFIDENT_TEXTILE,
                neuralWeaveMetrics = weaveMetrics,
            )
        } catch (_: Throwable) {
            ClassifierVerdict(
                hints = emptyList(),
                predictedCategory = signature?.category,
                calibratedConfidence = signature?.categoryConfidence ?: 0.35f,
                isOutOfDistribution = false,
            )
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }

    /** Executes `ghanaian_fabric_classifier.tflite` (`[1, 128, 128, 3]` float32 -> `[1, 17]` float32). */
    private fun runGhanaianClassifier(bmp: Bitmap): FloatArray? {
        val tflite = ghanaianInterpreter ?: return null
        val input = bitmapToFloatBuffer(bmp, GHANAIAN_INPUT_SIZE)
        val output = Array(1) { FloatArray(GHANAIAN_NUM_CLASSES) }
        synchronized(tflite) { tflite.run(input, output) }
        return output[0]
    }

    /** Executes `weave_motif_extractor.tflite` (`[1, 128, 128, 3]` float32 -> `[1, 6]` float32). */
    fun runWeaveExtractor(bmp: Bitmap): FloatArray? {
        val tflite = weaveInterpreter ?: return null
        val input = bitmapToFloatBuffer(bmp, WEAVE_INPUT_SIZE)
        val output = Array(1) { FloatArray(6) }
        synchronized(tflite) { tflite.run(input, output) }
        return output[0]
    }

    /** Executes `mobilenet_v2_1.0_224_quant.tflite` (`[1, 224, 224, 3]` uint8 -> `[1, 1001]` uint8). */
    private fun runImagenetOod(bmp: Bitmap): FloatArray? {
        val tflite = oodInterpreter ?: return null
        val scaled = Bitmap.createScaledBitmap(bmp, IMAGENET_INPUT_SIZE, IMAGENET_INPUT_SIZE, true)
        val input = ByteBuffer.allocateDirect(IMAGENET_INPUT_SIZE * IMAGENET_INPUT_SIZE * 3)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(IMAGENET_INPUT_SIZE * IMAGENET_INPUT_SIZE)
        scaled.getPixels(pixels, 0, IMAGENET_INPUT_SIZE, 0, 0, IMAGENET_INPUT_SIZE, IMAGENET_INPUT_SIZE)
        for (p in pixels) {
            input.put(((p shr 16) and 0xFF).toByte())
            input.put(((p shr 8) and 0xFF).toByte())
            input.put((p and 0xFF).toByte())
        }
        input.rewind()
        val output = Array(1) { ByteArray(IMAGENET_NUM_CLASSES) }
        synchronized(tflite) { tflite.run(input, output) }
        return FloatArray(IMAGENET_NUM_CLASSES) { i -> (output[0][i].toInt() and 0xFF) / 255f }
    }

    private fun bitmapToFloatBuffer(bmp: Bitmap, side: Int): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bmp, side, side, true)
        val buf = ByteBuffer.allocateDirect(4 * side * side * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(side * side)
        scaled.getPixels(pixels, 0, side, 0, 0, side, side)
        for (p in pixels) {
            buf.putFloat(((p shr 16) and 0xFF) / 255f)
            buf.putFloat(((p shr 8) and 0xFF) / 255f)
            buf.putFloat((p and 0xFF) / 255f)
        }
        buf.rewind()
        return buf
    }

    fun categoryBoost(hints: List<MlHint>): Pair<FabricCategory, Float>? {
        val best = hints.firstOrNull() ?: return null
        if (best.confidence < 0.20f) return null
        val mapped = mapLabelToCategory(best.label) ?: return null
        return mapped to best.confidence
    }

    companion object {
        private const val GHANAIAN_MODEL_ASSET = "ghanaian_fabric_classifier.tflite"
        private const val GHANAIAN_LABELS_ASSET = "ghanaian_fabric_labels.txt"
        private const val GHANAIAN_INPUT_SIZE = 128
        private const val GHANAIAN_NUM_CLASSES = 17

        private const val WEAVE_MODEL_ASSET = "weave_motif_extractor.tflite"
        private const val WEAVE_INPUT_SIZE = 128

        private const val IMAGENET_MODEL_ASSET = "mobilenet_v2_1.0_224_quant.tflite"
        private const val IMAGENET_LABELS_ASSET = "imagenet_labels.txt"
        private const val IMAGENET_INPUT_SIZE = 224
        private const val IMAGENET_NUM_CLASSES = 1001

        private const val TEMPERATURE = 0.65f
        private const val MIN_CALIBRATED_CONFIDENCE = 0.22f
        private const val MIN_TOP_MARGIN = 0.06f
        private const val MAX_NORMALIZED_ENTROPY = 0.88f

        data class UncertaintyEvaluation(
            val hints: List<MlHint>,
            val category: FabricCategory?,
            val calibratedConfidence: Float,
            val normalizedEntropy: Float,
            val topMargin: Float,
            val isConfidentTextile: Boolean,
            val reason: UncertaintyReason,
        )

        /**
         * Pure, unit-testable uncertainty & OOD evaluator over raw class scores.
         */
        fun evaluateUncertainty(
            rawScores: FloatArray,
            labels: List<String>,
        ): UncertaintyEvaluation {
            if (rawScores.size <= 1) {
                return UncertaintyEvaluation(
                    hints = emptyList(),
                    category = null,
                    calibratedConfidence = 0f,
                    normalizedEntropy = 1f,
                    topMargin = 0f,
                    isConfidentTextile = false,
                    reason = UncertaintyReason.LOW_TOP1_CONFIDENCE,
                )
            }
            val startIdx = if (rawScores.size == IMAGENET_NUM_CLASSES) 1 else 0
            val topIndices = (startIdx until rawScores.size)
                .sortedByDescending { rawScores[it] }
                .take(10)

            val maxLogit = topIndices.maxOf { rawScores[it] }
            val exps = FloatArray(topIndices.size) { i ->
                exp(((rawScores[topIndices[i]] - maxLogit) / TEMPERATURE).toDouble()).toFloat()
            }
            val sumExp = exps.sum().coerceAtLeast(1e-6f)
            val probs = FloatArray(exps.size) { i -> exps[i] / sumExp }

            var entropy = 0f
            for (p in probs) {
                if (p > 1e-6f) entropy -= p * ln(p.toDouble()).toFloat()
            }
            val maxEntropy = ln(probs.size.toDouble()).toFloat().coerceAtLeast(1e-4f)
            val normalizedEntropy = (entropy / maxEntropy).coerceIn(0f, 1f)

            val top1Prob = probs.getOrElse(0) { 0f }
            val top2Prob = probs.getOrElse(1) { 0f }
            val margin = (top1Prob - top2Prob).coerceAtLeast(0f)

            val hints = topIndices.take(3).mapIndexedNotNull { rank, classIdx ->
                val label = labels.getOrNull(classIdx)?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
                MlHint(label = label, confidence = rawScores[classIdx].coerceIn(0f, 1f))
            }

            val top1Label = hints.firstOrNull()?.label.orEmpty()
            val mappedTop1 = mapLabelToCategory(top1Label)
            val mappedAnyTop3 = hints.firstNotNullOfOrNull { mapLabelToCategory(it.label) }

            val reason = when {
                mappedTop1 == null && mappedAnyTop3 == null -> UncertaintyReason.NON_TEXTILE_DOMINANT
                rawScores[topIndices.first()] < MIN_CALIBRATED_CONFIDENCE -> UncertaintyReason.LOW_TOP1_CONFIDENCE
                normalizedEntropy > MAX_NORMALIZED_ENTROPY -> UncertaintyReason.HIGH_ENTROPY
                margin < MIN_TOP_MARGIN -> UncertaintyReason.SMALL_TOP1_TOP2_MARGIN
                else -> UncertaintyReason.CONFIDENT_TEXTILE
            }

            val confident = reason == UncertaintyReason.CONFIDENT_TEXTILE && (mappedTop1 ?: mappedAnyTop3) != null
            val calibrated = (rawScores[topIndices.first()] * (1f - 0.35f * normalizedEntropy)).coerceIn(0f, 0.85f)

            return UncertaintyEvaluation(
                hints = hints,
                category = if (confident) (mappedTop1 ?: mappedAnyTop3) else null,
                calibratedConfidence = calibrated,
                normalizedEntropy = normalizedEntropy,
                topMargin = margin,
                isConfidentTextile = confident,
                reason = reason,
            )
        }

        /**
         * Pure keyword-to-[FabricCategory] mapper covering both exact 17-class Ghanaian labels
         * and ImageNet textile/garment synsets.
         */
        fun mapLabelToCategory(label: String): FabricCategory? {
            val l = label.trim().lowercase()
            // Direct 17-class Ghanaian fabric label check first
            FabricCategory.values().firstOrNull { it.name.equals(l, ignoreCase = true) }?.let {
                if (it != FabricCategory.UNKNOWN) return it
            }
            return when {
                "velvet" in l -> FabricCategory.VELVET
                "wool" in l || "knit" in l || "cardigan" in l || "sweater" in l -> FabricCategory.WOOL
                "jean" in l || "denim" in l -> FabricCategory.DENIM
                "lace" in l || "doily" in l || "guipure" in l -> FabricCategory.LACE
                "damask" in l || "brocade" in l || "jacquard" in l || "aso-oke" in l || "asooke" in l -> FabricCategory.BROCADE
                "fugu" in l || "batakari" in l || "smock" in l -> FabricCategory.FUGU
                "gonja" in l || "daboya" in l -> FabricCategory.GONJA
                "kente_print" in l -> FabricCategory.KENTE_PRINT
                "kente" in l || "bonwire" in l || "agbamevo" in l || "stole" in l || "poncho" in l || "Tartan".lowercase() in l -> FabricCategory.KENTE
                "adinkra" in l -> FabricCategory.ADINKRA
                "batik" in l -> FabricCategory.BATIK
                "tiedye" in l || "tie-dye" in l || "adire" in l -> FabricCategory.TIEDYE
                "wax" in l -> FabricCategory.WAX
                "ankara" in l || "kitenge" in l || "pagne" in l || "sarong" in l || "kimono" in l || "abaya" in l || "overskirt" in l || "hoopskirt" in l -> FabricCategory.ANKARA
                "chiffon" in l || "Gown".lowercase() in l || "veil" in l || "tulle" in l -> FabricCategory.CHIFFON
                "silk" in l || "satin" in l || "pajama" in l || "robe" in l || "organza" in l -> FabricCategory.SILK
                "linen" in l || "dishrag" in l || "handkerchief" in l || "towel" in l -> FabricCategory.LINEN
                "cotton_plain" in l || "cotton" in l || "calico" in l || "poplin" in l -> FabricCategory.COTTON_PLAIN
                else -> null
            }
        }
    }
}
