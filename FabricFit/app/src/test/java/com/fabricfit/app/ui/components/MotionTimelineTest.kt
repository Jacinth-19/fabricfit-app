package com.fabricfit.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the properties that make the app read as one continuous timeline rather than a
 * series of per-screen animations.
 */
class MotionTimelineTest {

    @Test
    fun `every row is fully settled exactly when the timeline settles`() {
        // The whole point of the shared timeline: when navigation finishes, nothing is
        // still travelling. If a higher order ever lags, screens look sequenced.
        for (order in 0..8) {
            assertEquals(
                "row $order should be settled at the end",
                1f,
                settledProgressForRow(settled = 1f, order = order),
                0.0001f,
            )
        }
    }

    @Test
    fun `rows start hidden when the transition begins`() {
        for (order in 0..MotionSpec.ROW_STAGGER_STEPS) {
            assertEquals(
                "row $order should start at zero",
                0f,
                settledProgressForRow(settled = 0f, order = order),
                0.0001f,
            )
        }
    }

    @Test
    fun `progress is monotonic as the timeline advances`() {
        for (order in 0..MotionSpec.ROW_STAGGER_STEPS) {
            var previous = -1f
            var settled = 0f
            while (settled <= 1f) {
                val progress = settledProgressForRow(settled, order)
                assertTrue(
                    "row $order went backwards at $settled",
                    progress >= previous - 1e-4f,
                )
                previous = progress
                settled += 0.02f
            }
        }
    }

    @Test
    fun `higher rows begin later but never finish later`() {
        val early = settledProgressForRow(settled = 0.1f, order = 0)
        val late = settledProgressForRow(settled = 0.1f, order = 3)
        assertTrue("later rows should trail at the start", late <= early)

        val earlyEnd = settledProgressForRow(settled = 0.95f, order = 0)
        val lateEnd = settledProgressForRow(settled = 0.95f, order = 3)
        // Converging by the end is what keeps the hero and the rows in step.
        assertTrue("rows should converge, not diverge", lateEnd >= earlyEnd - 0.2f)
    }

    @Test
    fun `stagger saturates so a long list cannot outrun the timeline`() {
        // Without the cap, row 40 would not start until far past the settle duration.
        val capped = settledProgressForRow(settled = 0.5f, order = MotionSpec.ROW_STAGGER_STEPS)
        val overCapped = settledProgressForRow(settled = 0.5f, order = 500)
        assertEquals(capped, overCapped, 0.0001f)
    }

    @Test
    fun `out of range settled values are clamped rather than extrapolated`() {
        assertEquals(0f, settledProgressForRow(settled = -1f, order = 0), 0.0001f)
        assertEquals(1f, settledProgressForRow(settled = 4f, order = 0), 0.0001f)
    }

    @Test
    fun `hero travel outlasts the page settle so movement overlaps the swap`() {
        // If these ever invert, navigation becomes a crossfade with a jump at the end.
        assertTrue(MotionSpec.HERO_TRAVEL_MILLIS > MotionSpec.SCREEN_SETTLE_MILLIS)
    }
}
