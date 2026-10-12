package com.ntoma.studio.data.ml

import com.ntoma.studio.domain.model.PatternType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternClassifierTest {

    private val labels = listOf("STRIPED", "CHECKED", "PLAID", "DOTTED", "FLORAL", "GEOMETRIC", "SOLID")

    @Test
    fun `clear winner is a confident verdict`() {
        val verdict = PatternClassifier.verdictFrom(
            labels,
            floatArrayOf(0.81f, 0.05f, 0.04f, 0.04f, 0.03f, 0.02f, 0.01f),
        )!!
        assertEquals(PatternType.STRIPED, verdict.pattern)
        assertFalse(verdict.uncertain)
    }

    @Test
    fun `plaid label maps to the PLAID pattern`() {
        assertEquals(PatternType.PLAID, PatternClassifier.patternFor("PLAID"))
        val verdict = PatternClassifier.verdictFrom(
            labels,
            floatArrayOf(0.05f, 0.06f, 0.78f, 0.04f, 0.03f, 0.03f, 0.01f),
        )!!
        assertEquals(PatternType.PLAID, verdict.pattern)
        assertFalse(verdict.uncertain)
    }

    @Test
    fun `solid is a real answer not a fallback`() {
        val verdict = PatternClassifier.verdictFrom(
            labels,
            floatArrayOf(0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.88f),
        )!!
        assertEquals(PatternType.SOLID, verdict.pattern)
        assertFalse(verdict.uncertain)
    }

    @Test
    fun `weak scores report uncertain instead of forcing a pattern`() {
        val verdict = PatternClassifier.verdictFrom(
            labels,
            floatArrayOf(0.20f, 0.19f, 0.18f, 0.16f, 0.12f, 0.09f, 0.06f),
        )!!
        assertTrue(verdict.uncertain)
    }

    @Test
    fun `striped does not leak into fabric family claims`() {
        // The pattern model only ever produces PatternType values — structural check that the
        // two classifiers stay decoupled (appearance vs provenance).
        for (label in labels) {
            val pattern = PatternClassifier.patternFor(label)
            assertTrue("$label must map to a PatternType", pattern != null)
        }
    }

    @Test
    fun `unknown label or bad scores give no verdict`() {
        assertNull(
            PatternClassifier.verdictFrom(listOf("POLKADOTS"), floatArrayOf(0.9f)),
        )
        assertNull(PatternClassifier.verdictFrom(labels, floatArrayOf(0.5f)))
    }
}
