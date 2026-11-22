package com.fabricfit.app.media.garment

import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.GarmentSilhouette
import com.fabricfit.app.domain.model.LengthOption
import com.fabricfit.app.domain.model.NecklineOption
import com.fabricfit.app.domain.model.SleeveOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the body-fitted try-on geometry. These run off-device (no Bitmap, no
 * TFLite) so a broken outline fails CI before it ever reaches a phone.
 *
 * The masks below mirror a full-body front-view photo: head, neck, torso, two legs, with the
 * arms separated from the torso by a gap (the pose the FitRoom guidance asks for).
 */
class BodyFitTest {

    private val width = 400
    private val height = 800

    /**
     * A full-body front-view silhouette, proportioned like a real photo: the head is ~13 %
     * of the figure, the shoulders reach their widest ~24 % down, the waist pinches at
     * ~42 %, the hips flare to ~52 %, and the legs separate below that. Arms hang beside
     * the torso with a visible gap so they stay out of the torso measurement.
     */
    private fun mask(
        torsoLeft: Int = 150,
        torsoRight: Int = 250,
        arms: Boolean = true,
    ): IntArray {
        val alpha = IntArray(width * height)
        fun fillRow(y: Int, from: Int, to: Int) {
            if (y !in 0 until height) return
            for (x in from.coerceAtLeast(0)..to.coerceAtMost(width - 1)) alpha[y * width + x] = 255
        }
        // head: 60..150, 80 px wide
        for (y in 60..150) fillRow(y, 160, 240)
        // neck
        for (y in 151..175) fillRow(y, 185, 215)
        // torso: shoulders 176..225 widening to 62 px half-width, waist at 357, hips at 427
        for (y in 176..500) {
            val half = when {
                y <= 225 -> 30 + (y - 176) * 32 / 49
                y <= 357 -> 62 - (y - 225) * 4 / 132
                y <= 427 -> 58 + (y - 357) * 12 / 70
                else -> 72
            }
            fillRow(y, 200 - half, 200 + half)
        }
        // legs with a gap between them
        for (y in 501..760) {
            fillRow(y, torsoLeft, 194)
            fillRow(y, 206, torsoRight)
        }
        if (arms) {
            // arms hanging clear of the torso: detached runs outside the hip line
            for (y in 281..560) {
                fillRow(y, 90, 118)
                fillRow(y, 282, 310)
            }
        }
        return alpha
    }

    @Test
    fun `measure finds the body envelope and torso centre`() {
        val body = BodyFit.measure(mask(), width, height)
        assertNotNull(body)
        body!!
        assertEquals(60f, body.top, 1f)
        assertEquals(760f, body.bottom, 1f)
        assertEquals(200f, body.centre, 1f)
        assertTrue(body.usable)
        assertEquals(700f, body.personHeight, 2f)
        // Shoulder row sits at 24.5 % of the person: inside the torso, clear of the arms.
        assertEquals(61f, body.shoulderHalf, 1.5f)
        assertEquals(69f, body.hipHalf, 1.5f)
        assertTrue("waist must be narrower than the hips", body.waistHalf < body.hipHalf)
    }

    @Test
    fun `measure returns null for an empty mask`() {
        assertNull(BodyFit.measure(IntArray(width * height), width, height))
        assertNull(BodyFit.measure(IntArray(4), width, height))
    }

    @Test
    fun `measure returns null for masks with no body envelope`() {
        val empty = IntArray(width * height)
        assertNull(BodyFit.measure(empty, width, height))

        // A uniform 20x20 blob has an envelope but is far too small for a garment.
        val blob = IntArray(width * height)
        for (y in 100 until 120) for (x in 100 until 120) blob[y * width + x] = 255
        val body = BodyFit.measure(blob, width, height)
        assertNotNull(body)
        assertTrue("a 20 px blob must not count as a body", !body!!.usable)
    }

    @Test
    fun `dress outline covers the torso and is symmetric about the centre`() {
        val body = BodyFit.measure(mask(), width, height)!!
        val shapes = BodyFit.shapes(body, GarmentSilhouette.KABA)
        assertEquals(1, shapes.size)
        val outline = shapes.first()
        val xs = outline.map { it.first }
        val ys = outline.map { it.second }
        val left = xs.min()
        val right = xs.max()
        val centre = (left + right) / 2f
        assertEquals("outline should be centred on the body", 200f, centre, 2f)
        // The kaba hem lands around the knee: below the fingertips, above the ankles.
        assertTrue("hem y=${ys.max()}", ys.max() in 660f..720f)
        // The silhouette is wider than the torso and narrower than the outflung arms.
        val torsoWidth = 2 * 61f
        assertTrue("garment width=${right - left}", right - left > torsoWidth * 1.3f && right - left < 260f)
        // Top edge is the shoulder seam (~23.5 % down the person), not the top of the head.
        assertTrue("top y=${ys.min()}", ys.min() in 200f..250f)
    }

    @Test
    fun `sleeveless removes the arm panels and puff adds them back wider`() {
        val body = BodyFit.measure(mask(), width, height)!!
        val sleeveless = BodyFit.shapes(
            body,
            GarmentSilhouette.KABA,
            DesignCustomization(sleeve = SleeveOption.SLEEVELESS),
        )
        val puff = BodyFit.shapes(
            body,
            GarmentSilhouette.KABA,
            DesignCustomization(sleeve = SleeveOption.PUFF),
        )
        assertEquals(1, sleeveless.size)
        assertEquals(3, puff.size) // bodice + two sleeves
        // Compare the widest point at shoulder height: below the bust the skirt of the
        // dress is wider than any sleeve, so a bounding box would hide the difference.
        val shoulderY = body.top + body.personHeight * 0.235f
        fun shoulderSpan(shapes: List<Poly>): Float {
            val pts = shapes.flatMap { it }.filter { it.second in shoulderY - 2f..shoulderY + body.personHeight * 0.12f }
            return pts.maxOf { it.first } - pts.minOf { it.first }
        }
        val puffSpan = shoulderSpan(puff)
        val plainSpan = shoulderSpan(sleeveless)
        assertTrue("puff ($puffSpan) should be wider than sleeveless ($plainSpan)", puffSpan > plainSpan + 10f)
    }

    @Test
    fun `length options move the hem without moving the shoulders`() {
        val body = BodyFit.measure(mask(), width, height)!!
        fun hem(length: LengthOption) =
            BodyFit.shapes(body, GarmentSilhouette.MAXI, DesignCustomization(length = length))
                .first().maxOf { it.second }

        val mini = hem(LengthOption.MINI)
        val midi = hem(LengthOption.MIDI)
        val maxi = hem(LengthOption.MAXI)
        assertTrue("mini=$mini midi=$midi maxi=$maxi", mini < midi && midi < maxi)

        val topOfMaxi = BodyFit.shapes(body, GarmentSilhouette.MAXI, DesignCustomization(length = LengthOption.MAXI))
            .first().minOf { it.second }
        val topOfMini = BodyFit.shapes(body, GarmentSilhouette.MAXI, DesignCustomization(length = LengthOption.MINI))
            .first().minOf { it.second }
        assertEquals(topOfMini, topOfMaxi, 0.5f)
    }

    @Test
    fun `necklines change the size of the chest opening`() {
        val body = BodyFit.measure(mask(), width, height)!!
        // The outline starts at the left shoulder seam, crosses the neck opening, then hits
        // the right shoulder seam; everything after that is the body of the dress.
        fun neckDip(neck: NecklineOption): Float {
            val outline = BodyFit.shapes(body, GarmentSilhouette.KABA, DesignCustomization(neckline = neck)).first()
            val shoulderY = outline.first().second
            var dip = shoulderY
            for (point in outline.drop(1)) {
                if (point.second > shoulderY + 1f && kotlin.math.abs(point.first - body.centre) < body.personHeight * 0.2f) {
                    dip = maxOf(dip, point.second)
                } else if (dip > shoulderY + 1f) {
                    break
                }
            }
            return dip
        }

        val high = neckDip(NecklineOption.HIGH)
        val round = neckDip(NecklineOption.ROUND)
        val boat = neckDip(NecklineOption.BOAT)
        val v = neckDip(NecklineOption.VNECK)
        // A V-neck must cut deepest and a high collar must sit highest.
        assertTrue("high=$high round=$round boat=$boat v=$v", high < round && round < v && high < boat)
        assertTrue("V-neck opening should be visible: $v vs round $round", v - round > body.personHeight * 0.015f)
    }

    @Test
    fun `bounds and accent band track the garment`() {
        val body = BodyFit.measure(mask(), width, height)!!
        val shapes = BodyFit.shapes(body, GarmentSilhouette.MAXI)
        val b = BodyFit.bounds(shapes)
        assertEquals(4, b.size)
        assertTrue(b[2] > b[0] && b[3] > b[1])
        val band = BodyFit.accentBand(b)
        assertTrue(band[1] > b[1] && band[3] < b[3])
    }

    @Test
    fun `a person standing off-centre gets a garment centred on them`() {
        val leftShift = 70
        val shifted = IntArray(width * height)
        val source = mask()
        for (y in 0 until height) {
            for (x in 0 until width - leftShift) {
                shifted[y * width + x] = source[y * width + x + leftShift]
            }
        }
        val body = BodyFit.measure(shifted, width, height)!!
        assertEquals(200f - leftShift, body.centre, 2f)
        val outline = BodyFit.shapes(body, GarmentSilhouette.KABA).first()
        val centre = (outline.minOf { it.first } + outline.maxOf { it.first }) / 2f
        assertEquals(body.centre, centre, 2f)
    }
}
