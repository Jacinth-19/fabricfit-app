package com.ntoma.studio.data.ml

import android.content.Context
import android.graphics.Bitmap
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Shared loader/runner for optional, fine-tuned TFLite models shipped as app assets.
 *
 * A trained model is a *(model asset, labels asset)* pair. Both files must be present for the
 * model to count as available; when either is missing the runner stays silent ([available] is
 * false, [classify] returns null) and the analysis pipeline keeps using the heuristic engine.
 * That is how the pattern and fabric-family models ship empty-handed today and switch on the
 * day real models land — no code change, no settings toggle.
 *
 * Contract (must match `tools/dataset/train_finetune.py` exports):
 *  - input: square float32 images, values in `(px / 127.5) - 1` (MobileNetV3 preprocessing)
 *  - output: one float per class, softmax probabilities in the labels-file order
 *  - quantization: dynamic-range (weights int8, inputs/outputs float32)
 */
class TrainedModelRunner(
    context: Context,
    modelAsset: String,
    labelsAsset: String,
    private val inputSize: Int,
) {

    /** One label per line, in model output order. Empty when the asset is missing. */
    val labels: List<String>

    private val interpreter: org.tensorflow.lite.Interpreter?

    init {
        var loaded: org.tensorflow.lite.Interpreter? = null
        var loadedLabels: List<String> = emptyList()
        try {
            loadedLabels = context.assets.open(labelsAsset).bufferedReader().use { r ->
                r.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (loadedLabels.isNotEmpty()) {
                val afd = context.assets.openFd(modelAsset)
                val buffer = FileInputStream(afd.fileDescriptor).channel.use { ch: FileChannel ->
                    ch.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length)
                }
                loaded = org.tensorflow.lite.Interpreter(buffer)
            }
        } catch (t: Throwable) {
            // Missing asset, corrupt model, or interpreter init failure: stay in heuristic mode.
            loaded?.close()
            loaded = null
            loadedLabels = emptyList()
        }
        interpreter = loaded
        labels = loadedLabels
    }

    /** True when both the model and its labels file are present and the interpreter started. */
    val available: Boolean get() = interpreter != null && labels.isNotEmpty()

    /** Softmax scores in [labels] order, or null when the model is unavailable or failed. */
    fun classify(bitmap: Bitmap): FloatArray? = try {
        val interp = interpreter ?: return null
        if (labels.isEmpty()) return null
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val px = IntArray(inputSize * inputSize)
        scaled.getPixels(px, 0, inputSize, 0, 0, inputSize, inputSize)
        for (p in px) {
            input.putFloat((((p shr 16) and 0xFF) / 127.5f) - 1f)
            input.putFloat((((p shr 8) and 0xFF) / 127.5f) - 1f)
            input.putFloat(((p and 0xFF) / 127.5f) - 1f)
        }
        input.rewind()
        val out = FloatArray(labels.size)
        interp.run(input, out)
        out
    } catch (t: Throwable) {
        null
    }

    companion object {
        /**
         * Refuse-to-guess thresholds shared by both trained classifiers. A verdict is reported
         * as *uncertain* — not forced onto the nearest class — when the top score is below
         * [MIN_TOP], or when the top two classes are near-ties below [NEAR_TIE_CAP].
         */
        const val MIN_TOP = 0.40f
        const val MIN_MARGIN = 0.10f
        const val NEAR_TIE_CAP = 0.60f

        /** True when [top] beats [second] convincingly enough to state as an answer. */
        fun isConfident(top: Float, second: Float): Boolean =
            top >= MIN_TOP && (top - second >= MIN_MARGIN || top >= NEAR_TIE_CAP)
    }
}
