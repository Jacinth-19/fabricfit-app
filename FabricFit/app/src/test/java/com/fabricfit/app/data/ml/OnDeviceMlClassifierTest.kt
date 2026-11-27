package com.fabricfit.app.data.ml

import com.fabricfit.app.domain.model.FabricCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceMlClassifierTest {

    @Test
    fun `maps ImageNet textile labels onto FabricCategory`() {
        assertEquals(FabricCategory.VELVET, OnDeviceMlClassifier.mapLabelToCategory("velvet"))
        assertEquals(FabricCategory.WOOL, OnDeviceMlClassifier.mapLabelToCategory("wool, woolen, woollen"))
        assertEquals(FabricCategory.KENTE, OnDeviceMlClassifier.mapLabelToCategory("stole"))
        assertEquals(FabricCategory.BROCADE, OnDeviceMlClassifier.mapLabelToCategory("damask"))
        assertEquals(FabricCategory.DENIM, OnDeviceMlClassifier.mapLabelToCategory("jean, blue jean, denim"))
        assertEquals(FabricCategory.LACE, OnDeviceMlClassifier.mapLabelToCategory("doily, d'oyley"))
        assertEquals(FabricCategory.FUGU, OnDeviceMlClassifier.mapLabelToCategory("fugu smock"))
        assertEquals(FabricCategory.GONJA, OnDeviceMlClassifier.mapLabelToCategory("gonja strip weave"))
        assertEquals(FabricCategory.ADINKRA, OnDeviceMlClassifier.mapLabelToCategory("adinkra stamped cloth"))
    }

    @Test
    fun `non-textile ImageNet labels return null so pixel fallback decides`() {
        assertNull(OnDeviceMlClassifier.mapLabelToCategory("golden retriever"))
        assertNull(OnDeviceMlClassifier.mapLabelToCategory("sports car"))
        assertNull(OnDeviceMlClassifier.mapLabelToCategory("espresso"))
    }

    @Test
    fun `calibrated uncertainty gate rejects non-textile OOD top label`() {
        val labels = listOf("laptop, laptop computer", "sports car", "espresso", "golden retriever")
        val rawScores = floatArrayOf(0.85f, 0.05f, 0.05f, 0.05f)
        val verdict = OnDeviceMlClassifier.evaluateUncertainty(rawScores, labels)
        assertFalse(verdict.isConfidentTextile)
        assertNull(verdict.category)
        assertEquals(OnDeviceMlClassifier.UncertaintyReason.NON_TEXTILE_DOMINANT, verdict.reason)
    }

    @Test
    fun `calibrated uncertainty gate rejects flat high-entropy distribution`() {
        val labels = listOf("velvet", "wool", "stole", "damask", "chiffon", "cardigan")
        val rawScores = floatArrayOf(0.17f, 0.17f, 0.17f, 0.17f, 0.16f, 0.16f)
        val verdict = OnDeviceMlClassifier.evaluateUncertainty(rawScores, labels)
        assertFalse(verdict.isConfidentTextile)
    }

    @Test
    fun `calibrated uncertainty gate accepts confident textile prediction`() {
        val labels = listOf("stole", "velvet", "wool", "laptop")
        val rawScores = floatArrayOf(0.92f, 0.04f, 0.02f, 0.02f)
        val verdict = OnDeviceMlClassifier.evaluateUncertainty(rawScores, labels)
        assertTrue(verdict.isConfidentTextile)
        assertEquals(FabricCategory.KENTE, verdict.category)
        assertEquals(OnDeviceMlClassifier.UncertaintyReason.CONFIDENT_TEXTILE, verdict.reason)
        assertTrue(verdict.calibratedConfidence > 0.25f)
    }

    @Test
    fun `raw top1 floor rejects near-zero scores but admits real fabric scores`() {
        // Measured with the real mobilenet_v1 gate: fabric-mapped images score 0.289..0.988,
        // flat colours and noise 0.016..0.711. 0.22 must sit between the two floors.
        val labels = listOf("stole", "velvet", "wool", "laptop")
        val tooFaint = OnDeviceMlClassifier.evaluateUncertainty(
            floatArrayOf(0.05f, 0.04f, 0.03f, 0.02f), labels,
        )
        assertEquals(
            OnDeviceMlClassifier.UncertaintyReason.LOW_TOP1_CONFIDENCE,
            tooFaint.reason,
        )
        assertFalse(tooFaint.isConfidentTextile)

        val realFabric = OnDeviceMlClassifier.evaluateUncertainty(
            floatArrayOf(0.34f, 0.02f, 0.02f, 0.01f), labels,
        )
        assertTrue(realFabric.isConfidentTextile)
        assertTrue(realFabric.calibratedConfidence > 0.22f)
    }

    @Test
    fun `entropy ceiling is measured rather than guessed`() {
        // 0.994 was chosen from the real model: bundled textiles run 0.91..0.99, noise and
        // flat colour 0.99..1.00. A tight-but-flat top-10 distribution must be rejected.
        val flat = listOf("stole", "velvet", "wool", "damask", "chiffon", "lace", "linen", "silk")
        val scores = FloatArray(flat.size) { 0.62f - it * 0.002f }
        val verdict = OnDeviceMlClassifier.evaluateUncertainty(scores, flat)
        assertEquals(OnDeviceMlClassifier.UncertaintyReason.HIGH_ENTROPY, verdict.reason)
        assertFalse(verdict.isConfidentTextile)
    }

    @Test
    fun `a confident textile top1 maps to its category`() {
        val labels = listOf("velvet", "stole", "wool", "denim")
        val verdict = OnDeviceMlClassifier.evaluateUncertainty(
            floatArrayOf(0.61f, 0.03f, 0.02f, 0.01f), labels,
        )
        assertTrue(verdict.isConfidentTextile)
        assertEquals(FabricCategory.VELVET, verdict.category)
        assertEquals(OnDeviceMlClassifier.UncertaintyReason.CONFIDENT_TEXTILE, verdict.reason)
    }
}
