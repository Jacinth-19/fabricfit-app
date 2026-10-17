package com.fabricfit.app.media

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Second- and third-generation vision signals, pure Kotlin and fully unit-testable:
 *
 *  - GLCM texture statistics (contrast / homogeneity / energy / correlation).
 *  - Kente woven-strip detector: periodic banding along one axis plus a multi-hue band palette.
 *  - Multi-orientation Gabor / directional edge energy (0°, 45°, 90°, 135°) measuring weave
 *    anisotropy and orthogonal warp/weft balance vs. organic diagonal flow.
 *  - Spectral motif & tailoring alignment analyser measuring strip repeat, harmonic purity,
 *    and cut-alignment advice for Ghanaian seamstresses/tailors.
 */
object TextureMetrics {

    data class Glcm(
        val contrast: Float,
        val homogeneity: Float,
        val energy: Float,
        val correlation: Float,
    )

    data class StripSignal(
        /** 0..1 — strength of periodic multi-hue banding. */
        val stripScore: Float,
        /** Repeat length in pixels along the banding axis; 0 when aperiodic. */
        val periodPx: Float,
        /** True when bands vary along x (vertical strips). */
        val horizontalAxis: Boolean,
        /** Distinct hue families with >= 8% share. */
        val hueBandCount: Int,
    )

    enum class AlignmentAdvice {
        /** Strong periodic warp/weft bands — align strips across seams (Kente, Fugu, Gonja). */
        MATCH_WARP_STRIPS,
        /** Large repeating central motifs — center medallions on bodice/skirt panels (Ankara/Adinkra). */
        CENTER_LARGE_MOTIF,
        /** High directional sheen/pile — cut all panels in the same nap direction (Velvet, Silk, Brocade). */
        DIRECTIONAL_NAP,
        /** Isotropic or solid weave — panels can be nested freely for minimal cloth waste. */
        FREE_CUT,
    }

    data class GaborOrientationProfile(
        /** Normalized filter energy at 0° (horizontal weft), 45°, 90° (vertical warp), 135°. */
        val energy0: Float,
        val energy45: Float,
        val energy90: Float,
        val energy135: Float,
        /** Dominant orientation in degrees: 0, 45, 90, or 135. */
        val dominantAngleDeg: Int,
        /** 0..1 — directional concentration of the weave or print. */
        val anisotropy: Float,
        /** 0..1 — ratio of orthogonal (0° + 90°) energy to total energy (high on woven looms). */
        val warpWeftBalance: Float,
    )

    data class SpectralMotifProfile(
        val stripWidthPx: Float,
        val motifRepeatX: Float,
        val motifRepeatY: Float,
        /** 0..1 — fundamental vs. harmonic spectral peak sharpness. */
        val harmonicPurity: Float,
        val alignmentAdvice: AlignmentAdvice,
    )

    /** Grey-level co-occurrence over offsets (1,0) and (0,1), symmetrised. */
    fun glcm(lum: FloatArray, w: Int, h: Int, levels: Int = 16): Glcm {
        val q = IntArray(lum.size) { ((lum[it] / 256f * levels).toInt()).coerceIn(0, levels - 1) }
        val matrix = FloatArray(levels * levels)
        var pairs = 0
        for (y in 0 until h - 1) {
            for (x in 0 until w - 1) {
                val i = y * w + x
                matrix[q[i] * levels + q[i + 1]]++ // (0,1)
                matrix[q[i + 1] * levels + q[i]]++
                matrix[q[i] * levels + q[i + w]]++ // (1,0)
                matrix[q[i + w] * levels + q[i]]++
                pairs += 4
            }
        }
        if (pairs == 0) return Glcm(0f, 1f, 1f, 0f)
        for (i in matrix.indices) matrix[i] /= pairs.toFloat()

        var contrast = 0.0
        var homogeneity = 0.0
        var energy = 0.0
        for (i in 0 until levels) {
            for (j in 0 until levels) {
                val p = matrix[i * levels + j]
                val d = (i - j)
                contrast += d * d * p
                homogeneity += p / (1 + d * d)
                energy += p * p
            }
        }
        var mi = 0.0
        var mj = 0.0
        for (i in 0 until levels) for (j in 0 until levels) {
            mi += i * matrix[i * levels + j]
            mj += j * matrix[i * levels + j]
        }
        var vi = 0.0
        var vj = 0.0
        for (i in 0 until levels) for (j in 0 until levels) {
            vi += (i - mi) * (i - mi) * matrix[i * levels + j]
            vj += (j - mj) * (j - mj) * matrix[i * levels + j]
        }
        val si = sqrt(vi)
        val sj = sqrt(vj)
        var corr = 0.0
        if (si > 1e-6 && sj > 1e-6) {
            for (i in 0 until levels) for (j in 0 until levels) {
                corr += (i - mi) * (j - mj) * matrix[i * levels + j]
            }
            corr /= (si * sj)
        }
        return Glcm(
            contrast = contrast.toFloat().coerceIn(0f, 60f),
            homogeneity = homogeneity.toFloat().coerceIn(0f, 1f),
            energy = energy.toFloat().coerceIn(0f, 1f),
            correlation = corr.toFloat().coerceIn(-1f, 1f),
        )
    }

    /**
     * Multi-orientation Gabor-inspired directional filter response at 0°, 45°, 90°, and 135°.
     * Distinguishes orthogonal hand-loom weaves (0°/90°) from twill/diagonal/organic motifs.
     */
    fun gaborOrientation(lum: FloatArray, w: Int, h: Int): GaborOrientationProfile {
        if (w < 4 || h < 4 || lum.size < w * h) {
            return GaborOrientationProfile(0.25f, 0.25f, 0.25f, 0.25f, 0, 0f, 0.5f)
        }
        var e0 = 0.0
        var e45 = 0.0
        var e90 = 0.0
        var e135 = 0.0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                // 0°: horizontal lines (vertical gradient dy)
                val g0 = abs((lum[(y + 1) * w + x] - lum[(y - 1) * w + x]) * 0.5f)
                // 90°: vertical strips (horizontal gradient dx)
                val g90 = abs((lum[y * w + (x + 1)] - lum[y * w + (x - 1)]) * 0.5f)
                // 45°: main diagonal
                val g45 = abs((lum[(y + 1) * w + (x + 1)] - lum[(y - 1) * w + (x - 1)]) * 0.3535f)
                // 135°: anti-diagonal
                val g135 = abs((lum[(y + 1) * w + (x - 1)] - lum[(y - 1) * w + (x + 1)]) * 0.3535f)
                e0 += g0 * g0
                e90 += g90 * g90
                e45 += g45 * g45
                e135 += g135 * g135
            }
        }
        val total = (e0 + e45 + e90 + e135).coerceAtLeast(1e-6)
        val n0 = (e0 / total).toFloat()
        val n45 = (e45 / total).toFloat()
        val n90 = (e90 / total).toFloat()
        val n135 = (e135 / total).toFloat()

        val energies = listOf(0 to n0, 45 to n45, 90 to n90, 135 to n135)
        val best = energies.maxBy { it.second }
        val minE = energies.minOf { it.second }
        val anisotropy = if (total < 1.0) 0f else ((best.second - minE) / (best.second + minE).coerceAtLeast(1e-5f)).coerceIn(0f, 1f)
        val warpWeft = if (total < 1.0) 0.5f else (n0 + n90).coerceIn(0f, 1f)

        return GaborOrientationProfile(
            energy0 = n0,
            energy45 = n45,
            energy90 = n90,
            energy135 = n135,
            dominantAngleDeg = best.first,
            anisotropy = anisotropy,
            warpWeftBalance = warpWeft,
        )
    }

    /**
     * Spectral DFT harmonic periodicity & motif scale extractor along both axes.
     */
    fun spectralMotifProfile(
        pixels: IntArray,
        w: Int,
        h: Int,
    ): SpectralMotifProfile {
        val strips = kenteStrips(pixels, w, h)
        val lum = FloatArray(pixels.size) { i ->
            val p = pixels[i]
            ((p shr 16 and 0xFF) * 0.299f) + ((p shr 8 and 0xFF) * 0.587f) + ((p and 0xFF) * 0.114f)
        }
        val gabor = gaborOrientation(lum, w, h)
        val (pxX, strX) = periodStrength(lum, w, h, horizontal = true)
        val (pxY, strY) = periodStrength(lum, w, h, horizontal = false)
        val purity = spectralHarmonicPurity(lum, w, h, horizontal = strX >= strY)

        val advice = when {
            strips.stripScore >= 0.48f || (max(strX, strY) > 0.65f && gabor.warpWeftBalance > 0.62f) ->
                AlignmentAdvice.MATCH_WARP_STRIPS
            max(pxX, pxY) >= min(w, h) * 0.25f ->
                AlignmentAdvice.CENTER_LARGE_MOTIF
            gabor.anisotropy > 0.55f ->
                AlignmentAdvice.DIRECTIONAL_NAP
            else ->
                AlignmentAdvice.FREE_CUT
        }

        return SpectralMotifProfile(
            stripWidthPx = if (strips.periodPx > 0f) strips.periodPx / 2f else 0f,
            motifRepeatX = pxX,
            motifRepeatY = pxY,
            harmonicPurity = purity,
            alignmentAdvice = advice,
        )
    }

    private fun spectralHarmonicPurity(lum: FloatArray, w: Int, h: Int, horizontal: Boolean): Float {
        val profile = if (horizontal) {
            FloatArray(w) { x ->
                var s = 0f
                for (y in 0 until h) s += lum[y * w + x]
                s / h
            }
        } else {
            FloatArray(h) { y ->
                var s = 0f
                for (x in 0 until w) s += lum[y * w + x]
                s / w
            }
        }
        val n = profile.size
        if (n < 8) return 0f
        val mean = profile.average().toFloat()
        val maxK = (n / 2).coerceAtMost(32)
        var maxPower = 0.0
        var totalPower = 0.0
        for (k in 1..maxK) {
            var re = 0.0
            var im = 0.0
            val omega = 2.0 * PI * k / n
            for (t in 0 until n) {
                val v = profile[t] - mean
                re += v * cos(omega * t)
                im -= v * sin(omega * t)
            }
            val p = re * re + im * im
            totalPower += p
            if (p > maxPower) maxPower = p
        }
        return if (totalPower < 1e-3) 0f else (maxPower / totalPower).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Kente woven-strip signal. Handwoven kente shows narrow strips with repeating motif bands:
     * strong luminance periodicity along one axis and several saturated hue families (gold,
     * green, red, black). Printed imitation can share the geometry but usually fewer hue bands
     * and weaker band edges; the score honestly reflects that ambiguity.
     */
    fun kenteStrips(pixels: IntArray, w: Int, h: Int): StripSignal {
        val lum = FloatArray(pixels.size)
        val hueFamilies = IntArray(6) // gold/yellow, green, red, blue/indigo, black, other
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16 and 0xFF) / 255f
            val g = (p shr 8 and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            val mx = max(r, max(g, b))
            val mn = min(r, min(g, b))
            val l = (mx + mn) / 2f
            lum[i] = l * 255f
            val d = mx - mn
            val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f)).coerceAtLeast(1e-4f)
            val family = when {
                l < 0.18f -> 4 // black
                s < 0.2f -> 5 // neutral
                else -> {
                    val hue = when {
                        mx == r -> ((g - b) / d).let { if (it < 0) it + 6 else it }
                        mx == g -> (b - r) / d + 2
                        else -> (r - g) / d + 4
                    } * 60f
                    when {
                        hue < 20f || hue >= 340f -> 2 // red
                        hue < 70f -> 0 // gold/yellow
                        hue < 170f -> 1 // green
                        hue < 270f -> 3 // blue/indigo
                        else -> 2 // red/maroon side of purple
                    }
                }
            }
            hueFamilies[family]++
        }
        val total = pixels.size.toFloat()
        val hueBandCount = hueFamilies.take(5).count { it / total >= 0.08f }

        val (periodX, strengthX) = periodStrength(lum, w, h, horizontal = true)
        val (periodY, strengthY) = periodStrength(lum, w, h, horizontal = false)
        val horizontalAxis = strengthX >= strengthY
        val period = if (horizontalAxis) periodX else periodY
        val strength = max(strengthX, strengthY)

        // Band edges: sharp luminance jumps along the banding axis.
        var edges = 0
        var samples = 0
        for (y in 1 until h - 1 step 2) {
            for (x in 1 until w - 1 step 2) {
                val i = y * w + x
                if (horizontalAxis) {
                    if (abs(lum[i] - lum[i + 1]) > 36f) edges++
                } else {
                    if (abs(lum[i] - lum[i + w]) > 36f) edges++
                }
                samples++
            }
        }
        val edgeRate = if (samples == 0) 0f else edges.toFloat() / samples

        val periodic = if (strength > 0.55f && period > 0f) (strength - 0.55f) / 0.45f else 0f
        // More than two hue families is what separates kente cloth from ordinary stripes.
        val hueRich = ((hueBandCount - 2).coerceIn(0, 2)) / 2f
        val edged = (edgeRate / 0.3f).coerceIn(0f, 1f)
        val score = (0.35f * periodic + 0.45f * hueRich + 0.2f * edged).coerceIn(0f, 1f)
        return StripSignal(score, period, horizontalAxis, hueBandCount)
    }

    /** Best autocorrelation peak (value + its lag) along one axis; value in 0..1. */
    private fun periodStrength(lum: FloatArray, w: Int, h: Int, horizontal: Boolean): Pair<Float, Float> {
        val profile = if (horizontal) {
            FloatArray(w) { x ->
                var s = 0f
                for (y in 0 until h) s += lum[y * w + x]
                s / h
            }
        } else {
            FloatArray(h) { y ->
                var s = 0f
                for (x in 0 until w) s += lum[y * w + x]
                s / w
            }
        }
        val n = profile.size
        val maxLag = n / 2
        if (maxLag < 4) return 0f to 0f
        val m = profile.average()
        var denom = 0.0
        for (v in profile) denom += (v - m) * (v - m)
        if (denom < 1e-3) return 0f to 0f
        var best = 0f
        var bestLag = 0f
        for (lag in 3..maxLag) {
            var num = 0.0
            for (i in 0 until n - lag) num += (profile[i] - m) * (profile[i + lag] - m)
            val c = (num / denom).toFloat()
            if (c > best) {
                best = c
                bestLag = lag.toFloat()
            }
        }
        return bestLag to best.coerceIn(0f, 1f)
    }
}
