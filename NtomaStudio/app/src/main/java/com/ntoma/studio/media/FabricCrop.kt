package com.ntoma.studio.media

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/** Normalised selection in the EXIF-oriented image, independent of screen density. */
data class FabricCrop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun pixels(width: Int, height: Int): PixelCrop {
        require(width > 0 && height > 0)
        val x = floor(left.coerceIn(0f, 1f) * width).toInt().coerceIn(0, width - 1)
        val y = floor(top.coerceIn(0f, 1f) * height).toInt().coerceIn(0, height - 1)
        val r = ceil(right.coerceIn(0f, 1f) * width).toInt().coerceIn(x + 1, width)
        val b = ceil(bottom.coerceIn(0f, 1f) * height).toInt().coerceIn(y + 1, height)
        return PixelCrop(x, y, r - x, b - y)
    }

    companion object {
        val FULL = FabricCrop(0f, 0f, 1f, 1f)
        val CENTRE = FabricCrop(.35f, .35f, .65f, .65f)
    }
}

data class PixelCrop(val x: Int, val y: Int, val width: Int, val height: Int)

/** The same ContentScale.Fit geometry used to draw and hit-test the image. */
data class FittedImage(val left: Float, val top: Float, val width: Float, val height: Float) {
    fun imageX(x: Float) = ((x - left) / width).coerceIn(0f, 1f)
    fun imageY(y: Float) = ((y - top) / height).coerceIn(0f, 1f)

    companion object {
        fun inView(imageW: Int, imageH: Int, viewW: Float, viewH: Float): FittedImage {
            val scale = min(viewW / imageW, viewH / imageH)
            val w = imageW * scale
            val h = imageH * scale
            return FittedImage((viewW - w) / 2, (viewH - h) / 2, w, h)
        }
    }
}
