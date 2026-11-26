package com.fabricfit.app.media.garment

import com.fabricfit.app.domain.model.DesignCustomization
import com.fabricfit.app.domain.model.GarmentSilhouette
import com.fabricfit.app.domain.model.LengthOption
import com.fabricfit.app.domain.model.NecklineOption
import com.fabricfit.app.domain.model.SleeveOption
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Body-fitted garment outlines for the on-device try-on.
 *
 * The catalogue [GarmentGeometry] polygons are normalised illustrations: they are drawn
 * inside a fixed 20–80 % box, which is much narrower than a real torso. Compositing them
 * over a photo produces a strip of cloth floating over the chest instead of a garment.
 *
 * [BodyFit] measures shoulder / bust / waist / hip from the segmentation mask the
 * compositor already produces and builds a dress outline that follows those landmarks.
 * Everything here is pure math over an alpha mask, so it is unit-testable off-device.
 */
object BodyFit {

    /**
     * How far below the widest point the cloth settles on the deltoid: real fabric droops
     * rather than cutting a perfect horizontal line from the shoulder tip to the neck.
     */
    private const val SHOULDER_JOINT_DROP = 0.030f

    /** Landmark measurements in source-image pixels. */
    data class Body(
        val top: Float,
        val bottom: Float,
        val centre: Float,
        val shoulderHalf: Float,
        val bustHalf: Float,
        val waistHalf: Float,
        val hipHalf: Float,
        val thighHalf: Float,
        /**
         * True when the figure fills enough of the frame and the measured widths could be a
         * person. Callers fall back to the illustration polygons when this is false, so a
         * cropped or distant subject never gets a sliver of cloth glued to it.
         */
        val usable: Boolean,
    ) {
        val personHeight: Float get() = (bottom - top).coerceAtLeast(1f)
    }

    /**
     * Measures the person from an alpha mask (`0..255`, `width * height` entries).
     * Widths are taken from the horizontal run that contains the body centre, so a
     * detached arm or flowing hair cannot inflate the torso width.
     */
    fun measure(maskAlpha: IntArray, width: Int, height: Int): Body? {
        if (maskAlpha.size < width * height || width <= 0 || height <= 0) return null

        var top = height
        var bottom = 0
        for (y in 0 until height) {
            var rowHasPerson = false
            val rowStart = y * width
            for (x in 0 until width) {
                if ((maskAlpha[rowStart + x] and 0xFF) > 128) {
                    rowHasPerson = true
                    break
                }
            }
            if (rowHasPerson) {
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        if (bottom <= top) return null
        val personHeight = (bottom - top).toFloat()

        // Torso centre: median x of the chest band, then snapped to its run centre.
        val bandTop = top + (personHeight * 0.28f).toInt()
        val bandBottom = top + (personHeight * 0.44f).toInt()
        val xs = ArrayList<Int>()
        for (y in bandTop until min(bandBottom, height)) {
            val rowStart = y * width
            for (x in 0 until width) {
                if ((maskAlpha[rowStart + x] and 0xFF) > 128) xs.add(x)
            }
        }
        if (xs.isEmpty()) return null
        xs.sort()
        var centre = xs[xs.size / 2]
        runAt(maskAlpha, width, height, (bandTop + bandBottom) / 2, centre)?.let { (l, r) ->
            centre = (l + r) / 2
        }

        fun halfAt(fraction: Float): Float {
            val row = (top + personHeight * fraction).roundToInt().coerceIn(0, height - 1)
            val run = runAt(maskAlpha, width, height, row, centre) ?: return 0f
            return (run.second - run.first) / 2f
        }

        val body = Body(
            top = top.toFloat(),
            bottom = bottom.toFloat(),
            centre = centre.toFloat(),
            shoulderHalf = halfAt(0.245f),
            bustHalf = max(halfAt(0.30f), halfAt(0.33f)),
            waistHalf = min(halfAt(0.40f), halfAt(0.44f)),
            hipHalf = max(halfAt(0.48f), halfAt(0.52f)),
            thighHalf = halfAt(0.62f),
            usable = false,
        )
        // A usable measurement needs three things: a body envelope that fills a decent part
        // of the frame, widths that could be a person, and a shoulder wider than the neck.
        val envelope = personHeight / height
        val plausibleWidths = body.shoulderHalf > personHeight * 0.04f &&
            body.shoulderHalf < personHeight * 0.35f
        return body.copy(usable = envelope >= 0.25f && plausibleWidths)
    }

    /**
     * A dress outline clamped to the body landmarks, in source-image pixels.
     *
     * The measured widths include the arms when the wearer stands with arms down, so they
     * are scaled back to torso proportions before the outline is built:
     * bust ≈ 0.84·shoulder, waist ≈ 0.70·shoulder, hip ≈ 0.94·shoulder.
     */
    fun shapes(
        body: Body,
        silhouette: GarmentSilhouette,
        customization: DesignCustomization = DesignCustomization(),
    ): List<Poly> {
        val top = body.top
        val ph = body.personHeight
        val cx = body.centre

        // The mask includes the arms when they hang beside the body, so its widths are wider
        // than a torso. Trust the measurement but pull it toward body proportions: the arms
        // alone made the shoulder exactly as wide as the bust, leaving no shoulder shelf.
        // When the caller passed estimated widths (no segmenter), the estimate is already
        // torso-shaped, so it is used as-is.
        val torsoPrior = body.bustHalf * 0.58f
        val shoulder = if (body.usable) {
            max((body.shoulderHalf * 0.93f + torsoPrior) / 2f, ph * 0.085f)
        } else {
            max(body.shoulderHalf * 0.93f, ph * 0.085f)
        }
        val bust = min(max(shoulder * 0.84f, torsoPrior), shoulder * 0.98f)
        val waist = min(max(shoulder * 0.70f, body.waistHalf * 0.50f), bust * 0.90f)
        val hip = min(max(shoulder * 0.94f, body.hipHalf * 0.62f), bust * 1.22f)
        val thigh = max(body.thighHalf * 1.05f, hip * 0.95f)

        val yShoulder = top + ph * 0.235f
        val yBust = top + ph * 0.335f
        val yWaist = top + ph * 0.425f
        val yHip = top + ph * 0.525f

        val length = customization.length
        val hemFraction = when (length) {
            LengthOption.MINI -> 0.665f
            LengthOption.MIDI -> 0.80f
            LengthOption.MAXI -> 0.965f
            LengthOption.FLOOR -> 1.0f
            LengthOption.DEFAULT -> silhouetteHemFraction(silhouette)
        }
        val flare = when (length) {
            LengthOption.MINI -> 1.04f
            LengthOption.MIDI -> 1.16f
            LengthOption.MAXI -> 1.30f
            LengthOption.FLOOR -> 1.38f
            LengthOption.DEFAULT -> 1.22f
        }
        val yHem = top + ph * hemFraction
        val hem = max(hip, thigh) * flare

        // Neck opening as (half-width, dip) relative to the shoulder width and body height.
        // A neckline that spans most of the shoulder reads as an off-shoulder dress, so the
        // deep-V and boat styles stay near a third of the shoulder width.
        // Neck opening as (half-width, dip). It is drawn from the left neck edge across a
        // notched opening to the right neck edge; below ~0.30 of the shoulder the two edges
        // end up a few pixels apart and the "opening" disappears.
        val neck = when (customization.neckline) {
            NecklineOption.VNECK, NecklineOption.V_NECK -> 0.32f to 0.085f
            NecklineOption.HIGH -> 0.24f to 0.014f
            NecklineOption.OFF_SHOULDER -> 0.50f to 0.030f
            NecklineOption.BOAT -> 0.44f to 0.020f
            NecklineOption.ROUND, NecklineOption.DEFAULT -> 0.34f to 0.050f
        }
        val neckW = shoulder * neck.first
        val neckY = yShoulder + ph * neck.second

        val right = smooth(
            listOf(
                cx + shoulder to yShoulder + ph * SHOULDER_JOINT_DROP,
                cx + bust to yBust,
                cx + waist to yWaist,
                cx + hip to yHip,
                cx + (hip + hem) / 2f to yHip + (yHem - yHip) * 0.58f,
                cx + hem to yHem,
            ),
        )
        val left = right.map { (x, y) -> (2 * cx - x) to y }.reversed()

        val outline = ArrayList<Pair<Float, Float>>()
        // The top edge runs from the left shoulder tip to the left side of the neck and then
        // back out to the right tip. Without the tip vertex the neck opening spans the whole
        // shoulder line and leaves the bare photo visible through the shoulders.
        outline.add(cx - shoulder to yShoulder + ph * SHOULDER_JOINT_DROP)
        // Left neck edge. Without this vertex the outline jumps straight from the left
        // shoulder tip to the middle of the opening, so the fill cuts a diagonal across
        // the left chest and the person's own clothing shows through where a collar
        // should be. The desktop port (fitted_garment_polys) has always emitted it.
        outline.add(
            if (customization.neckline == NecklineOption.OFF_SHOULDER) {
                cx - neckW to yShoulder - ph * 0.004f
            } else {
                cx - neckW to yShoulder
            },
        )
        when (customization.neckline) {
            NecklineOption.VNECK, NecklineOption.V_NECK -> outline.add(cx to neckY)
            else -> {
                outline.add(cx - neckW * 0.55f to yShoulder + ph * neck.second * 0.5f)
                outline.add(cx to neckY)
                outline.add(cx + neckW * 0.55f to yShoulder + ph * neck.second * 0.5f)
            }
        }
        outline.add(
            if (customization.neckline == NecklineOption.OFF_SHOULDER) {
                cx + neckW to yShoulder - ph * 0.004f
            } else {
                cx + neckW to yShoulder
            },
        )
        outline.addAll(right)
        outline.addAll(left)

        val shapes = ArrayList<Poly>()
        shapes.add(outline)

        // Sleeves hang off the deltoid so they overlay the arms rather than the chest.
        val sleeveDrop = when (customization.sleeve) {
            SleeveOption.PUFF -> 0.155f
            SleeveOption.SHORT -> 0.17f
            SleeveOption.THREE_QUARTER -> 0.36f
            SleeveOption.LONG -> 0.50f
            else -> null
        }
        if (sleeveDrop != null) {
            val puff = if (customization.sleeve == SleeveOption.PUFF) 0.46f else 0.34f
            val taper = if (customization.sleeve == SleeveOption.PUFF) 0.30f else 0.22f
            // The cap starts exactly on the bodice's shoulder seam, so the two outlines meet
            // instead of crossing: a crossing leaves a visible sliver at the shoulder.
            val yTop = yShoulder + ph * SHOULDER_JOINT_DROP
            val yBottom = yShoulder + ph * sleeveDrop
            for (side in intArrayOf(-1, 1)) {
                val sx = cx + side * shoulder * 0.90f
                val points = ArrayList<Pair<Float, Float>>()
                points.add(cx + side * shoulder to yTop)
                val steps = 14
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    // sin() gives an eased bulge, so the cap is round at the top and tapers to the cuff
                    val bulge = kotlin.math.sin(Math.PI * (0.15 + 0.85 * t)).toFloat()
                    val outer = shoulder * (puff * 0.55f + taper * 0.45f * bulge)
                    points.add(sx + side * outer to yTop + (yBottom - yTop) * t)
                }
                // Walk back up the armhole to the shoulder seam and close there, so the cap
                // overlaps the bodice instead of leaving a notch at the shoulder joint.
                for (i in 0..steps) {
                    val t = 1f - i.toFloat() / steps
                    val inner = shoulder * (puff * 0.02f + 0.06f * kotlin.math.sin(Math.PI * t).toFloat())
                    points.add(sx - side * inner to yTop + (yBottom - yTop) * t)
                }
                // The mirrored sleeve comes out counter-clockwise; keep every panel in the
                // same winding so fills and outline strokes behave identically on both sides.
                val panel = smooth(points, samples = 3)
                shapes.add(if (side > 0) panel else panel.reversed())
            }
        }

        if (customization.neckline == NecklineOption.HIGH) {
            shapes.add(
                listOf(
                    cx - neckW * 1.04f to yShoulder - ph * 0.022f,
                    cx + neckW * 1.04f to yShoulder - ph * 0.022f,
                    cx + neckW * 1.04f to yShoulder + ph * 0.022f,
                    cx - neckW * 1.04f to yShoulder + ph * 0.022f,
                ),
            )
        }
        return shapes
    }

    /** Bounding box of [shapes] as `[left, top, right, bottom]`, for shading passes. */
    fun bounds(shapes: List<Poly>): FloatArray {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        shapes.forEach { poly ->
            poly.forEach { (x, y) ->
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        if (left > right) return floatArrayOf(0f, 0f, 1f, 1f)
        return floatArrayOf(left, top, right, bottom)
    }

    /** Placement band for the ACCENTS fabric placement, in pixels. */
    fun accentBand(bounds: FloatArray): FloatArray {
        val height = bounds[3] - bounds[1]
        return floatArrayOf(bounds[0], bounds[1] + height * 0.44f, bounds[2], bounds[1] + height * 0.54f)
    }

    private fun silhouetteHemFraction(silhouette: GarmentSilhouette): Float = when (silhouette) {
        GarmentSilhouette.PRINT_SHIRT -> 0.66f
        GarmentSilhouette.MERMAID, GarmentSilhouette.PEPLUM, GarmentSilhouette.SENATOR -> 0.88f
        GarmentSilhouette.MAXI, GarmentSilhouette.GOWN, GarmentSilhouette.KAFTAN,
        GarmentSilhouette.AGBADA, GarmentSilhouette.COVER_UP, GarmentSilhouette.SMOCK -> 0.965f
        else -> 0.90f
    }

    /** Catmull-Rom resampling so the outline reads as cloth rather than a polygon. */
    private fun smooth(points: List<Pair<Float, Float>>, samples: Int = 10): Poly {
        if (points.size < 3) return points
        val pts = listOf(points.first()) + points + listOf(points.last())
        val out = ArrayList<Pair<Float, Float>>()
        for (i in 1 until pts.size - 2) {
            val (x0, y0) = pts[i - 1]
            val (x1, y1) = pts[i]
            val (x2, y2) = pts[i + 1]
            val (x3, y3) = pts[i + 2]
            for (s in 0 until samples) {
                val t = s.toFloat() / samples
                val t2 = t * t
                val t3 = t2 * t
                val x = 0.5f * ((2 * x1) + (-x0 + x2) * t + (2 * x0 - 5 * x1 + 4 * x2 - x3) * t2 + (-x0 + 3 * x1 - 3 * x2 + x3) * t3)
                val y = 0.5f * ((2 * y1) + (-y0 + y2) * t + (2 * y0 - 5 * y1 + 4 * y2 - y3) * t2 + (-y0 + 3 * y1 - 3 * y2 + y3) * t3)
                out.add(x to y)
            }
        }
        out.add(points.last())
        return out
    }

    /** Widest horizontal run in [row] containing [centreX] (or nearest to it). */
    private fun runAt(maskAlpha: IntArray, width: Int, height: Int, row: Int, centreX: Int): Pair<Int, Int>? {
        if (row !in 0 until height) return null
        val rowStart = row * width
        var best: Pair<Int, Int>? = null
        var nearest: Pair<Int, Int>? = null
        var nearestDistance = Int.MAX_VALUE
        var runStart = -1
        for (x in 0..width) {
            val on = x < width && (maskAlpha[rowStart + x] and 0xFF) > 128
            if (on && runStart < 0) {
                runStart = x
            } else if (!on && runStart >= 0) {
                val run = runStart to (x - 1)
                if (run.first <= centreX && centreX <= run.second) {
                    if (best == null || (run.second - run.first) > (best!!.second - best!!.first)) best = run
                }
                val distance = min(kotlin.math.abs(run.first - centreX), kotlin.math.abs(run.second - centreX))
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearest = run
                }
                runStart = -1
            }
        }
        return best ?: nearest
    }
}
