package com.ntoma.studio.data.ml

import com.ntoma.studio.domain.model.FabricCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FabricFamilyClassifierTest {

    private val labels = listOf("KENTE", "ANKARA", "ADINKRA", "LACE", "DENIM")

    @Test
    fun `clear winner is a confident verdict`() {
        val verdict = FabricFamilyClassifier.verdictFrom(
            labels,
            floatArrayOf(0.72f, 0.10f, 0.08f, 0.06f, 0.04f),
        )!!
        assertEquals(FabricCategory.KENTE, verdict.category)
        assertEquals(0.72f, verdict.confidence)
        assertFalse(verdict.uncertain)
    }

    @Test
    fun `low top score reports uncertain instead of forcing a family`() {
        val verdict = FabricFamilyClassifier.verdictFrom(
            labels,
            floatArrayOf(0.24f, 0.22f, 0.20f, 0.19f, 0.15f),
        )!!
        assertTrue("weak scores must be uncertain", verdict.uncertain)
    }

    @Test
    fun `near-tie below the cap reports uncertain`() {
        val verdict = FabricFamilyClassifier.verdictFrom(
            labels,
            floatArrayOf(0.46f, 0.44f, 0.05f, 0.03f, 0.02f),
        )!!
        assertTrue("a near-tie must not be forced", verdict.uncertain)
    }

    @Test
    fun `strong near-tie above the cap stays confident`() {
        val verdict = FabricFamilyClassifier.verdictFrom(
            labels,
            floatArrayOf(0.64f, 0.30f, 0.03f, 0.02f, 0.01f),
        )!!
        assertFalse(verdict.uncertain)
    }

    @Test
    fun `unknown label maps to no verdict`() {
        val verdict = FabricFamilyClassifier.verdictFrom(
            listOf("GOLDEN_KRAMEN", "KENTE"),
            floatArrayOf(0.9f, 0.1f),
        )
        assertNull("labels outside FabricCategory must not force an answer", verdict)
    }

    @Test
    fun `labels are matched case-insensitively`() {
        assertEquals(
            FabricCategory.KENTE_PRINT,
            FabricFamilyClassifier.categoryFor("kente_print"),
        )
        assertEquals(FabricCategory.WAX, FabricFamilyClassifier.categoryFor(" Wax "))
        assertNull(FabricFamilyClassifier.categoryFor("goldfish"))
    }

    @Test
    fun `mismatched or empty scores give no verdict`() {
        assertNull(FabricFamilyClassifier.verdictFrom(labels, floatArrayOf(0.5f, 0.5f)))
        assertNull(FabricFamilyClassifier.verdictFrom(emptyList(), floatArrayOf()))
    }
}
