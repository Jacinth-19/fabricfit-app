package com.fabricfit.app.media.garment

import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.GarmentSilhouette
import com.fabricfit.app.domain.model.LengthOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the numbers the local reference renderer
 * (`tools/vton_qa/vton_local.py`) produced on the bundled Ghanaian sample model.
 *
 * If either implementation drifts, the phone and the laptop stop producing the same
 * try-on; this test fails first. Values come from `measure_body()` and
 * `fitted_garment_polys()` in the Python file, run against `deeplabv3_257_mv.tflite` on
 * `catalog/samples/model_ama_fullbody.jpg` at the 558x1000 render size.
 */
class BodyFitParityTest {

    // 558 x 1000 render of the bundled model photo, measured by the local pipeline.
    private val frameW = 558
    private val frameH = 1000

    @Test
    fun `python-parity - sample model landmarks`() {
        // Build a mask with the same envelope the real mask has (top 66, bottom 945,
        // torso centre 278, shoulder half 102.5, bust 111.5, waist 112.5, hip 114.5).
        val alpha = IntArray(frameW * frameH)
        fun fill(y: Int, from: Int, to: Int) {
            if (y !in 0 until frameH) return
            for (x in from.coerceAtLeast(0)..to.coerceAtMost(frameW - 1)) alpha[y * frameW + x] = 255
        }
        // head
        for (y in 66..280) fill(y, 278 - 38, 278 + 38)
        // torso (arms merged with torso, as in the real arms-down pose)
        for (y in 281..518) {
            val t = (y - 281) / 237f
            val half = (102.5f + t * 12f).toInt()
            fill(y, 278 - half, 278 + half)
        }
        // legs
        for (y in 519..945) {
            fill(y, 204, 272)
            fill(y, 284, 357)
        }

        val body = BodyFit.measure(alpha, frameW, frameH)!!
        assertEquals(66f, body.top, 1f)
        assertEquals(945f, body.bottom, 1f)
        assertEquals(278f, body.centre, 2f)

        // The same clamps the Python renderer applies. The mask merges arms with the
        // torso, so the measured shoulder is pulled halfway toward torso proportions.
        val ph = body.personHeight
        val torsoPrior = body.bustHalf * 0.58f
        val shoulder = maxOf((body.shoulderHalf * 0.93f + torsoPrior) / 2f, ph * 0.085f)
        val bust = minOf(maxOf(shoulder * 0.84f, torsoPrior), shoulder * 0.98f)
        val waist = minOf(maxOf(shoulder * 0.70f, body.waistHalf * 0.50f), bust * 0.90f)
        val hip = minOf(maxOf(shoulder * 0.94f, body.hipHalf * 0.62f), bust * 1.22f)
        // Measured on the real deeplabv3 mask of the bundled model at 558x1000.
        assertEquals("shoulder", 80.0f, shoulder, 2.5f)
        assertEquals("bust", 67.2f, bust, 2.5f)
        assertEquals("waist", 56.2f, waist, 2.5f)
        assertEquals("hip", 75.2f, hip, 2.5f)

        val shapes = BodyFit.shapes(
            body,
            GarmentSilhouette.KABA,
            DesignCustomization(neckline = com.fabricfit.app.domain.model.NecklineOption.VNECK),
        )
        val outline = shapes.first()
        // Garment width across the bust is ~140 px on a 558 px frame (25 %).
        val widthAtBust = run {
            val bustY = body.top + ph * 0.335f
            val row = outline.filter { kotlin.math.abs(it.second - bustY) < ph * 0.02f }
            assertTrue("no outline points near the bust line", row.isNotEmpty())
            row.maxOf { it.first } - row.minOf { it.first }
        }
        assertEquals("bust-line width", 139.7f, widthAtBust, 12f)
        assertTrue("garment must cover the torso, not a strip", widthAtBust > 110f)
    }

    /**
     * The outline must reach the neck on BOTH sides of the opening. A missing left neck
     * edge makes the fill jump from the left shoulder tip straight to the middle of the
     * opening: on-device that drew a diagonal across the left chest and left the person's
     * own top showing through the shoulder, while the desktop port rendered fine.
     */
    @Test
    fun `outline visits both neck edges for every neckline`() {
        val alpha = IntArray(frameW * frameH)
        for (y in 66..945) {
            for (x in 180..380) alpha[y * frameW + x] = 255
        }
        val body = BodyFit.measure(alpha, frameW, frameH)!!
        val ph = body.personHeight
        val shoulderY = body.top + ph * 0.235f

        for (neckline in com.fabricfit.app.domain.model.NecklineOption.entries) {
            val outline = BodyFit.shapes(
                body,
                GarmentSilhouette.KABA,
                DesignCustomization(neckline = neckline),
            ).first()
            // Vertices on the shoulder line, one on each side of the centre.
            val onLine = outline.filter { kotlin.math.abs(it.second - shoulderY) < ph * 0.01f }
            val leftEdge = onLine.filter { it.first < body.centre - 1f }
            val rightEdge = onLine.filter { it.first > body.centre + 1f }
            assertTrue("$neckline: no left neck edge on the shoulder line", leftEdge.isNotEmpty())
            assertTrue("$neckline: no right neck edge on the shoulder line", rightEdge.isNotEmpty())
            // Both tips must also be present, further out than the neck edges.
            val leftTip = onLine.filter {
                it.first < leftEdge.minOf { e -> e.first } - 1f
            }
            val rightTip = onLine.filter {
                it.first > rightEdge.maxOf { e -> e.first } + 1f
            }
            assertTrue("$neckline: no left shoulder tip", leftTip.isNotEmpty())
            assertTrue("$neckline: no right shoulder tip", rightTip.isNotEmpty())
        }
    }

    @Test
    fun `hem length fractions match the reference renderer`() {
        // vton_local.py: MINI 0.665, MIDI 0.80, MAXI 0.965, FLOOR 1.0
        val expected = mapOf(
            LengthOption.MINI to 0.665f,
            LengthOption.MIDI to 0.80f,
            LengthOption.MAXI to 0.965f,
            LengthOption.FLOOR to 1.0f,
        )
        val alpha = IntArray(frameW * frameH)
        for (y in 66..945) {
            for (x in 180..380) alpha[y * frameW + x] = 255
        }
        val body = BodyFit.measure(alpha, frameW, frameH)!!
        expected.forEach { (length, fraction) ->
            val hem = BodyFit.shapes(
                body,
                GarmentSilhouette.MAXI,
                DesignCustomization(length = length),
            ).first().maxOf { it.second }
            val actual = (hem - body.top) / body.personHeight
            assertEquals("hem fraction for $length", fraction, actual, 0.01f)
        }
    }
}
