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

    private fun mask(
        torsoLeft: Int = 150,
        torsoRight: Int = 250,
        shoulderHalf: Int = 62,
        headHalf: Int = 38,
        arms: Boolean = true,
    ): IntArray {
        val alpha = IntArray(width * height)
        fun fillRow(y: Int, from: Int, to: Int) {
            if (y !in 0 until height) return
            for (x in from.coerceAtLeast(0)..to.coerceAtMost(width - 1)) alpha[y * width + x] = 255
        }
        // head (top 22 % of the person)
        for (y in 60..240) fillRow(y, 200 - headHalf, 200 + headHalf)
        // neck
        for (y in 240..270) fillRow(y, 185, 215)
        // torso from shoulders to hips
        for (y in 270..500) {
            val t = (y - 270) / 230f
            val half = (shoulderHalf + t * 6).toInt()
            fillRow(y, 200 - half, 200 + half)
        }
        // legs
        for (y in 500..760) {
            fillRow(y, torsoLeft, 200 - 6)
            fillRow(y, 200 + 6, torsoRight)
        }
        if (arms) {
            // arms hanging beside the torso with a 6 px gap: detached runs
            for (y in 280..560) {
                fillRow(y, 110, 140)
                fillRow(y, 260, 290)
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
        // Shoulder band sits at ~24 % of the person: still inside the torso, not the arms.
        assertTrue("shoulderHalf=${body.shoulderHalf}", body.shoulderHalf in 50f..75f)
        assertTrue("hipHalf=${body.hipHalf}", body.hipHalf in 50f..80f)
    }

    @Test
    fun `measure returns null for an empty mask`() {
        assertNull(BodyFit.measure(IntArray(width * height), width, height))
        assertNull(BodyFit.measure(IntArray(4), width, height))
    }

    @Test
    fun `measure flags unusable masks instead of throwing`() {
        val tiny = IntArray(width * height)
        // 20x20 blob in the corner: valid mask, far too small to fit a garment to
        for (y in 0 until 20) for (x in 0 until 20) tiny[y * width + x] = 255
        val body = BodyFit.measure(tiny, width, height)
        assertTrue(body == null || !body.usable)
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
        // Shoulder line sits inside the measured deltoid width but wider than the neck.
        // Shoulder seam lands inside the measured deltoid (124 px wide), not on the arms.
        val shoulderWidth = right - left
        assertTrue("shoulder width=$shoulderWidth", shoulderWidth in 110f..140f)
        // Hem is near the ankles, well below the hip.
        assertTrue("hem y=${ys.max()}", ys.max() > 700f)
        assertTrue("top y=${ys.min()}", ys.min() in 210f..240f)
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
        val puffWidth = BodyFit.bounds(puff).let { it[2] - it[0] }
        val plainWidth = BodyFit.bounds(sleeveless).let { it[2] - it[0] }
        assertTrue("puff ($puffWidth) should be wider than sleeveless ($plainWidth)", puffWidth > plainWidth)
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
        fun opening(neck: NecklineOption): Float {
            val outline = BodyFit.shapes(body, GarmentSilhouette.KABA, DesignCustomization(neckline = neck)).first()
            // the neckline vertices are the first few points before the shoulder seam
            return outline.take(7).minOf { it.second }
        }
        val high = opening(NecklineOption.HIGH)
        val round = opening(NecklineOption.ROUND)
        val boat = opening(NecklineOption.BOAT)
        val v = opening(NecklineOption.VNECK)
        // A V-neck must cut deepest; a high collar must sit highest. Round/boat sit between.
        assertTrue("high=$high round=$round boat=$boat v=$v", high < round && round < v && high < boat)
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
            for (x in leftShift until width) {
                shifted[y * width + x] = source[y * width + x - leftShift]
            }
        }
        val body = BodyFit.measure(shifted, width, height)!!
        assertEquals(200f - leftShift, body.centre, 2f)
        val outline = BodyFit.shapes(body, GarmentSilhouette.KABA).first()
        val centre = (outline.minOf { it.first } + outline.maxOf { it.first }) / 2f
        assertEquals(body.centre, centre, 2f)
    }
}
