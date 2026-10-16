package com.fabricfit.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

/**
 * Multi-model on-device Human Parser, Selfie Segmenter, Skin-Tone Extractor & Neural Cloth Shader:
 *
 *  1. **MediaPipe Selfie Segmentation (`selfie_segmentation.tflite`)**:
 *     `256x256x3 float32 -> [1, 256, 256, 1] float32` person foreground probability map.
 *  2. **DeepLabV3 PASCAL VOC (`deeplabv3_257_mv.tflite`)**:
 *     `257x257x3 float32 -> [1, 257, 257, 21] float32` full-body person segmentation fallback.
 *  3. **5-Class Human Body-Zone Parser (`human_parsing_5class.tflite`)**:
 *     `128x128x3 float32 -> [1, 128, 128, 5] float32` separating `BACKGROUND`, `HEAD_AND_FACE`,
 *     `UPPER_GARMENT`, `LOWER_GARMENT`, and `SKIN_EXPOSED`.
 *  4. **Neural Cloth Warper & Drape Shading (`cloth_warper_shading.tflite`)**:
 *     `64x64x4 float32 -> [1, 64, 64, 3] float32` predicting `[warp_dx, warp_dy, shade_gain]`.
 */
class PersonSegmenter(private val context: Context) {

    enum class BodyRegion {
        BACKGROUND,
        HEAD_AND_FACE,
        UPPER_GARMENT,
        LOWER_GARMENT,
        SKIN_EXPOSED,
    }

    enum class SkinUndertone {
        WARM_GOLDEN,
        COOL_DEEP,
        NEUTRAL_RICH,
    }

    data class HumanParseResult(
        val width: Int,
        val height: Int,
        val regionMap: ByteArray,
        val dominantSkinArgb: Long?,
        val skinUndertone: SkinUndertone?,
    )

    private val selfieInterpreter: Interpreter? by lazy {
        loadAssetInterpreter(SELFIE_MODEL_ASSET)
    }

    private val deeplabInterpreter: Interpreter? by lazy {
        loadAssetInterpreter(DEEPLAB_MODEL_ASSET)
    }

    private val humanParsingInterpreter: Interpreter? by lazy {
        loadAssetInterpreter(HUMAN_PARSING_ASSET)
    }

    private val clothShadingInterpreter: Interpreter? by lazy {
        loadAssetInterpreter(CLOTH_SHADING_ASSET)
    }

    private fun loadAssetInterpreter(assetName: String): Interpreter? =
        runCatching {
            val afd = context.assets.openFd(assetName)
            val buf = FileInputStream(afd.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength,
            )
            Interpreter(buf, Interpreter.Options().apply { setNumThreads(2) })
        }.getOrNull()

    /**
     * Returns an `ALPHA_8`-style `ARGB_8888` bitmap at `(targetWidth, targetHeight)` where
     * alpha = 255 on person pixels and 0 on background, fusing `selfie_segmentation.tflite`
     * with `deeplabv3_257_mv.tflite`.
     */
    fun personMask(person: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap? {
        // Try MediaPipe Selfie Segmentation (256x256) first for crisp boundaries
        val selfieMask = runSelfieMask(person, targetWidth, targetHeight)
        if (selfieMask != null) return selfieMask
        return runDeepLabMask(person, targetWidth, targetHeight)
    }

    private fun runSelfieMask(person: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap? {
        val tflite = selfieInterpreter ?: return null
        return try {
            val scaled = Bitmap.createScaledBitmap(person, SELFIE_SIZE, SELFIE_SIZE, true)
            val input = ByteBuffer.allocateDirect(4 * SELFIE_SIZE * SELFIE_SIZE * 3)
                .order(ByteOrder.nativeOrder())
            val px = IntArray(SELFIE_SIZE * SELFIE_SIZE)
            scaled.getPixels(px, 0, SELFIE_SIZE, 0, 0, SELFIE_SIZE, SELFIE_SIZE)
            for (c in px) {
                input.putFloat(((c shr 16) and 0xFF) / 255f)
                input.putFloat(((c shr 8) and 0xFF) / 255f)
                input.putFloat((c and 0xFF) / 255f)
            }
            input.rewind()

            val output = Array(1) { Array(SELFIE_SIZE) { Array(SELFIE_SIZE) { FloatArray(1) } } }
            synchronized(tflite) { tflite.run(input, output) }

            val maskSmall = Bitmap.createBitmap(SELFIE_SIZE, SELFIE_SIZE, Bitmap.Config.ARGB_8888)
            val outPx = IntArray(SELFIE_SIZE * SELFIE_SIZE)
            var personCount = 0
            for (y in 0 until SELFIE_SIZE) {
                val row = output[0][y]
                for (x in 0 until SELFIE_SIZE) {
                    val prob = row[x][0]
                    val isPerson = prob >= 0.45f
                    if (isPerson) personCount++
                    val alpha = (prob.coerceIn(0f, 1f) * 255f).roundToInt()
                    outPx[y * SELFIE_SIZE + x] = if (isPerson) Color.argb(alpha, 0, 0, 0) else 0
                }
            }
            if (personCount < SELFIE_SIZE * SELFIE_SIZE / 30) return null
            maskSmall.setPixels(outPx, 0, SELFIE_SIZE, 0, 0, SELFIE_SIZE, SELFIE_SIZE)
            Bitmap.createScaledBitmap(maskSmall, targetWidth, targetHeight, true)
        } catch (_: Throwable) {
            null
        }
    }

    private fun runDeepLabMask(person: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap? {
        val tflite = deeplabInterpreter ?: return null
        return try {
            val scaled = Bitmap.createScaledBitmap(person, DEEPLAB_SIZE, DEEPLAB_SIZE, true)
            val input = ByteBuffer.allocateDirect(4 * DEEPLAB_SIZE * DEEPLAB_SIZE * 3)
                .order(ByteOrder.nativeOrder())
            val px = IntArray(DEEPLAB_SIZE * DEEPLAB_SIZE)
            scaled.getPixels(px, 0, DEEPLAB_SIZE, 0, 0, DEEPLAB_SIZE, DEEPLAB_SIZE)
            for (c in px) {
                input.putFloat(((c shr 16 and 0xFF) - 127.5f) / 127.5f)
                input.putFloat(((c shr 8 and 0xFF) - 127.5f) / 127.5f)
                input.putFloat(((c and 0xFF) - 127.5f) / 127.5f)
            }
            input.rewind()

            val output = Array(1) { Array(DEEPLAB_SIZE) { Array(DEEPLAB_SIZE) { FloatArray(DEEPLAB_CLASSES) } } }
            synchronized(tflite) { tflite.run(input, output) }

            val maskSmall = Bitmap.createBitmap(DEEPLAB_SIZE, DEEPLAB_SIZE, Bitmap.Config.ARGB_8888)
            val outPx = IntArray(DEEPLAB_SIZE * DEEPLAB_SIZE)
            var personCount = 0
            for (y in 0 until DEEPLAB_SIZE) {
                val row = output[0][y]
                for (x in 0 until DEEPLAB_SIZE) {
                    val scores = row[x]
                    var bestIdx = 0
                    var bestScore = scores[0]
                    for (k in 1 until DEEPLAB_CLASSES) {
                        if (scores[k] > bestScore) {
                            bestScore = scores[k]
                            bestIdx = k
                        }
                    }
                    val isPerson = bestIdx == PERSON_CLASS
                    if (isPerson) personCount++
                    outPx[y * DEEPLAB_SIZE + x] = if (isPerson) Color.BLACK else Color.TRANSPARENT
                }
            }
            if (personCount < DEEPLAB_SIZE * DEEPLAB_SIZE / 30) return null
            maskSmall.setPixels(outPx, 0, DEEPLAB_SIZE, 0, 0, DEEPLAB_SIZE, DEEPLAB_SIZE)
            Bitmap.createScaledBitmap(maskSmall, targetWidth, targetHeight, true)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Runs `human_parsing_5class.tflite` (`[1, 128, 128, 3]` -> `[1, 128, 128, 5]`) fused with
     * YCbCr skin-tone extraction to produce a full [HumanParseResult].
     */
    fun parsePerson(person: Bitmap): HumanParseResult? {
        val maskBmp = personMask(person, person.width, person.height) ?: return null
        val p = IntArray(person.width * person.height)
        person.getPixels(p, 0, person.width, 0, 0, person.width, person.height)
        val m = IntArray(person.width * person.height)
        maskBmp.getPixels(m, 0, person.width, 0, 0, person.width, person.height)
        val maskAlpha = IntArray(m.size) { i -> (m[i] ushr 24) and 0xFF }

        val baseParse = analyzeHumanRegionsAndSkin(p, maskAlpha, person.width, person.height)
        val tflite = humanParsingInterpreter ?: return baseParse

        return try {
            val scaled = Bitmap.createScaledBitmap(person, PARSING_SIZE, PARSING_SIZE, true)
            val input = ByteBuffer.allocateDirect(4 * PARSING_SIZE * PARSING_SIZE * 3)
                .order(ByteOrder.nativeOrder())
            val px = IntArray(PARSING_SIZE * PARSING_SIZE)
            scaled.getPixels(px, 0, PARSING_SIZE, 0, 0, PARSING_SIZE, PARSING_SIZE)
            for (c in px) {
                input.putFloat(((c shr 16) and 0xFF) / 255f)
                input.putFloat(((c shr 8) and 0xFF) / 255f)
                input.putFloat((c and 0xFF) / 255f)
            }
            input.rewind()
            val output = Array(1) { Array(PARSING_SIZE) { Array(PARSING_SIZE) { FloatArray(5) } } }
            synchronized(tflite) { tflite.run(input, output) }

            val refinedRegions = baseParse.regionMap.copyOf()
            val w = person.width
            val h = person.height
            for (y in 0 until h) {
                val sy = (y * PARSING_SIZE / h).coerceIn(0, PARSING_SIZE - 1)
                for (x in 0 until w) {
                    val i = y * w + x
                    if (maskAlpha[i] <= 128) continue
                    val sx = (x * PARSING_SIZE / w).coerceIn(0, PARSING_SIZE - 1)
                    val probs = output[0][sy][sx]
                    if (probs[1] > 0.55f && y < h * 0.32f) {
                        refinedRegions[i] = BodyRegion.HEAD_AND_FACE.ordinal.toByte()
                    }
                }
            }
            baseParse.copy(regionMap = refinedRegions)
        } catch (_: Throwable) {
            baseParse
        }
    }

    /**
     * Runs `cloth_warper_shading.tflite` (`[1, 64, 64, 4]` -> `[1, 64, 64, 3]`) to predict a
     * 64x64 3D luminance fold shading gain map (`0.5..1.25`) over the person + garment mask.
     */
    fun predictClothShadingMap(person: Bitmap): FloatArray? {
        val tflite = clothShadingInterpreter ?: return null
        return try {
            val scaled = Bitmap.createScaledBitmap(person, SHADING_SIZE, SHADING_SIZE, true)
            val input = ByteBuffer.allocateDirect(4 * SHADING_SIZE * SHADING_SIZE * 4)
                .order(ByteOrder.nativeOrder())
            val px = IntArray(SHADING_SIZE * SHADING_SIZE)
            scaled.getPixels(px, 0, SHADING_SIZE, 0, 0, SHADING_SIZE, SHADING_SIZE)
            for (i in px.indices) {
                val c = px[i]
                val x = (i % SHADING_SIZE).toFloat() / SHADING_SIZE
                val y = (i / SHADING_SIZE).toFloat() / SHADING_SIZE
                val inGarmentZone = if (x in 0.2f..0.8f && y in 0.26f..0.95f) 1f else 0f
                input.putFloat(((c shr 16) and 0xFF) / 255f)
                input.putFloat(((c shr 8) and 0xFF) / 255f)
                input.putFloat((c and 0xFF) / 255f)
                input.putFloat(inGarmentZone)
            }
            input.rewind()
            val output = Array(1) { Array(SHADING_SIZE) { Array(SHADING_SIZE) { FloatArray(3) } } }
            synchronized(tflite) { tflite.run(input, output) }
            val gains = FloatArray(SHADING_SIZE * SHADING_SIZE)
            for (y in 0 until SHADING_SIZE) {
                for (x in 0 until SHADING_SIZE) {
                    gains[y * SHADING_SIZE + x] = output[0][y][x][2].coerceIn(0.45f, 1.15f)
                }
            }
            gains
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        private const val SELFIE_MODEL_ASSET = "selfie_segmentation.tflite"
        private const val SELFIE_SIZE = 256

        private const val DEEPLAB_MODEL_ASSET = "deeplabv3_257_mv.tflite"
        private const val DEEPLAB_SIZE = 257
        private const val DEEPLAB_CLASSES = 21
        private const val PERSON_CLASS = 15

        private const val HUMAN_PARSING_ASSET = "human_parsing_5class.tflite"
        private const val PARSING_SIZE = 128

        private const val CLOTH_SHADING_ASSET = "cloth_warper_shading.tflite"
        const val SHADING_SIZE = 64

        /**
         * Pure pixel transform (unit-testable without Bitmap): copies `person` pixels where
         * `maskAlpha[i] <= 128` (background) and leaves foreground transparent (`0`).
         */
        fun backgroundPixels(person: IntArray, maskAlpha: IntArray): IntArray {
            require(person.size >= maskAlpha.size)
            return IntArray(person.size) { i ->
                if (i >= maskAlpha.size || maskAlpha[i] <= 128) person[i] else 0
            }
        }

        /**
         * Pure, unit-testable multiclass human region & skin-tone analyzer.
         */
        fun analyzeHumanRegionsAndSkin(
            personPixels: IntArray,
            maskAlpha: IntArray,
            width: Int,
            height: Int,
        ): HumanParseResult {
            require(personPixels.size == width * height && maskAlpha.size == width * height)
            val regionMap = ByteArray(width * height)

            var minY = height
            var maxY = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (maskAlpha[y * width + x] > 128) {
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
            if (minY >= maxY) {
                return HumanParseResult(width, height, regionMap, null, null)
            }

            val personHeight = (maxY - minY).coerceAtLeast(1)
            val headBottomY = minY + (personHeight * 0.22f).roundToInt()
            val torsoBottomY = minY + (personHeight * 0.58f).roundToInt()

            var skinR = 0L
            var skinG = 0L
            var skinB = 0L
            var skinCount = 0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    if (maskAlpha[idx] <= 128) {
                        regionMap[idx] = BodyRegion.BACKGROUND.ordinal.toByte()
                        continue
                    }
                    val c = personPixels[idx]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val isSkin = isLikelySkinPixel(r, g, b)

                    val region = when {
                        y <= headBottomY -> BodyRegion.HEAD_AND_FACE
                        isSkin && (y <= torsoBottomY) -> BodyRegion.SKIN_EXPOSED
                        y <= torsoBottomY -> BodyRegion.UPPER_GARMENT
                        else -> BodyRegion.LOWER_GARMENT
                    }
                    regionMap[idx] = region.ordinal.toByte()

                    if (isSkin && (region == BodyRegion.HEAD_AND_FACE || region == BodyRegion.SKIN_EXPOSED)) {
                        skinR += r
                        skinG += g
                        skinB += b
                        skinCount++
                    }
                }
            }

            if (skinCount == 0) {
                return HumanParseResult(width, height, regionMap, null, null)
            }

            val avgR = (skinR / skinCount).toInt().coerceIn(0, 255)
            val avgG = (skinG / skinCount).toInt().coerceIn(0, 255)
            val avgB = (skinB / skinCount).toInt().coerceIn(0, 255)
            val skinArgb = (0xFFL shl 24) or (avgR.toLong() shl 16) or (avgG.toLong() shl 8) or avgB.toLong()

            val warmthIndex = (avgR - avgB) - (avgR - avgG) * 0.5f
            val undertone = when {
                warmthIndex > 22f -> SkinUndertone.WARM_GOLDEN
                warmthIndex < 8f -> SkinUndertone.COOL_DEEP
                else -> SkinUndertone.NEUTRAL_RICH
            }

            return HumanParseResult(
                width = width,
                height = height,
                regionMap = regionMap,
                dominantSkinArgb = skinArgb,
                skinUndertone = undertone,
            )
        }

        internal fun isLikelySkinPixel(r: Int, g: Int, b: Int): Boolean {
            val y = 0.299f * r + 0.587f * g + 0.114f * b
            if (y < 28f || y > 242f) return false
            val cb = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b
            val cr = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b
            return cb in 77f..135f && cr in 130f..178f && r >= g && g >= (b - 12)
        }
    }
}
