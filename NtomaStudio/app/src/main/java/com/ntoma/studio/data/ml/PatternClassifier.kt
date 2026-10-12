package com.ntoma.studio.data.ml

import android.content.Context
import android.graphics.Bitmap
import com.ntoma.studio.domain.model.PatternType

/**
 * Trained visible-pattern classifier (model #2 of the recognition plan, docs/ml_roadmap.md) —
 * and the first one to be trained, because the newly acquired dataset labels appearance, not
 * provenance.
 *
 * Identifies what the cloth *looks like*: stripes, checks, plaid, dots, floral, geometric (plus
 * SOLID as the honest "no visible pattern" class). It never claims what the cloth *is* — a
 * striped photo may or may not be Kente, and floral does not automatically mean Ankara; family
 * is the [FabricFamilyClassifier]'s separate job.
 *
 * Optional asset pair ([MODEL] + [LABELS]) from `tools/dataset/train_finetune.py --task pattern`;
 * until the files exist [available] is false and the heuristic pattern pass keeps answering.
 */
class PatternClassifier(context: Context) {

    companion object {
        const val MODEL = "pattern_classifier.tflite"
        const val LABELS = "pattern_labels.txt"
        private const val INPUT = 224

        /** Label string -> pattern type. Labels mirror `label_schema_pattern.json` class names. */
        fun patternFor(label: String): PatternType? = try {
            PatternType.valueOf(label.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }

        /**
         * Pure verdict logic (unit-tested without Android): same honesty rule as the family
         * classifier — low top score or a near-tie reports *uncertain* rather than forcing a
         * pattern. Null when the scores are unusable or the top label is unknown to the app.
         */
        fun verdictFrom(labels: List<String>, scores: FloatArray): PatternVerdict? {
            if (scores.isEmpty() || scores.size != labels.size) return null
            val ranked = scores.indices.sortedByDescending { scores[it] }
            val topIdx = ranked.first()
            val top = scores[topIdx]
            val second = if (ranked.size > 1) scores[ranked[1]] else 0f
            val pattern = patternFor(labels[topIdx]) ?: return null
            val uncertain = !TrainedModelRunner.isConfident(top, second)
            return PatternVerdict(pattern, top, uncertain)
        }
    }

    private val runner = TrainedModelRunner(context, MODEL, LABELS, INPUT)

    /** False until the fine-tuned model assets are shipped. */
    val available: Boolean get() = runner.available

    /** Verdict for [bitmap], or null when no trained model is present or it failed to run. */
    fun classify(bitmap: Bitmap): PatternVerdict? {
        val scores = runner.classify(bitmap) ?: return null
        return verdictFrom(runner.labels, scores)
    }
}

/** What the trained pattern model sees. [uncertain] means "don't quote a pattern". */
data class PatternVerdict(
    val pattern: PatternType,
    val confidence: Float,
    val uncertain: Boolean,
)
