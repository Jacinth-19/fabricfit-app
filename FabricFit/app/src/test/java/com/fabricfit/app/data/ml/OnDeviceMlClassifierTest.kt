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
        val labels = listOf("laptop, laptop computer", "velvet", "stole", "damask")
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
        val rawScores = floatArrayOf(0.78f, 0.08f, 0.07f, 0.07f)
        val verdict = OnDeviceMlClassifier.evaluateUncertainty(rawScores, labels)
        assertTrue(verdict.isConfidentTextile)
        assertEquals(FabricCategory.KENTE, verdict.category)
        assertEquals(OnDeviceMlClassifier.UncertaintyReason.CONFIDENT_TEXTILE, verdict.reason)
        assertTrue(verdict.calibratedConfidence > 0.25f)
    }
}
