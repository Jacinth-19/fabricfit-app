package com.ntoma.studio.data.ml

import android.content.Context
import android.graphics.Bitmap
import com.ntoma.studio.domain.model.FabricCategory

/**
 * Trained fabric-family classifier (model #1 of the recognition plan, docs/ml_roadmap.md).
 *
 * Suggests Kente, Kente-print, Ankara/wax, Adinkra, batik, lace, denim, … from a selected
 * cloth or dress photo. The contract is honesty-first: every verdict carries a confidence, and
 * when the model cannot tell, it says *uncertain* instead of forcing the nearest family.
 *
 * The model is an optional asset pair ([MODEL] + [LABELS]) produced by
 * `tools/dataset/train_finetune.py --task fabric`. Until those files exist [available] is false
 * and the pipeline keeps using the heuristic engine — nothing about this class forces an answer.
 *
 * Note: "uncertain" collapses to [FabricCategory.UNKNOWN] in the domain model; the confidence
 * still carries the model's honest top score. Appearance ("striped", "floral") is deliberately
 * NOT used here — that is the [PatternClassifier]'s job.
 */
class FabricFamilyClassifier(context: Context) {

    companion object {
        const val MODEL = "fabric_family_classifier.tflite"
        const val LABELS = "fabric_family_labels.txt"
        private const val INPUT = 224

        /** Label string -> fabric family. Labels mirror `label_schema.json` class names. */
        fun categoryFor(label: String): FabricCategory? = try {
            FabricCategory.valueOf(label.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }

        /**
         * Pure verdict logic (unit-tested without Android): rank the scores, map the top label,
         * and flag uncertainty instead of guessing. Returns null when the scores are unusable
         * or the top label maps to no family we speak about.
         */
        fun verdictFrom(labels: List<String>, scores: FloatArray): FamilyVerdict? {
            if (scores.isEmpty() || scores.size != labels.size) return null
            val ranked = scores.indices.sortedByDescending { scores[it] }
            val topIdx = ranked.first()
            val top = scores[topIdx]
            val second = if (ranked.size > 1) scores[ranked[1]] else 0f
            val category = categoryFor(labels[topIdx]) ?: return null
            val uncertain = !TrainedModelRunner.isConfident(top, second)
            return FamilyVerdict(category, top, uncertain)
        }
    }

    private val runner = TrainedModelRunner(context, MODEL, LABELS, INPUT)

    /** False until the fine-tuned model assets are shipped. */
    val available: Boolean get() = runner.available

    /** Verdict for [bitmap], or null when no trained model is present or it failed to run. */
    fun classify(bitmap: Bitmap): FamilyVerdict? {
        val scores = runner.classify(bitmap) ?: return null
        return verdictFrom(runner.labels, scores)
    }
}

/** What the trained fabric-family model thinks. [uncertain] means "don't quote a family". */
data class FamilyVerdict(
    val category: FabricCategory,
    val confidence: Float,
    val uncertain: Boolean,
)
