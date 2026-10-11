package com.ntoma.studio.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import com.ntoma.studio.domain.model.AppError
import com.ntoma.studio.domain.model.Outcome
import java.io.File

/**
 * Memory-safe image loading, validation, transformation and storage.
 * Everything the camera / photo-picker flows need, in one place.
 */
class ImageProcessor(private val context: Context) {

    companion object {
        private const val MAX_PIXELS = 20_000_000L // ~20MP guard before we even decode
        const val EDIT_MAX_EDGE = 1600
        const val SAVE_MAX_EDGE = 1280
        const val JPEG_QUALITY = 88
    }

    fun validate(uri: Uri): Outcome<Uri> {
        val type = context.contentResolver.getType(if (uri.scheme == null) Uri.fromFile(java.io.File(uri.toString())) else uri)
        if (type != null && !type.startsWith("image/")) return Outcome.Failure(AppError.UnsupportedImage)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(if (uri.scheme == null) Uri.fromFile(java.io.File(uri.toString())) else uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return Outcome.Failure(AppError.InvalidImage)
        } catch (e: Exception) {
            return Outcome.Failure(AppError.InvalidImage)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return Outcome.Failure(AppError.InvalidImage)
        if (bounds.outWidth.toLong() * bounds.outHeight > MAX_PIXELS * 4) return Outcome.Failure(AppError.ImageTooLarge)
        return Outcome.Success(uri)
    }

    fun load(uri: Uri, maxEdge: Int): Outcome<Bitmap> {
        return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(if (uri.scheme == null) Uri.fromFile(java.io.File(uri.toString())) else uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return Outcome.Failure(AppError.InvalidImage)
        if (options.outWidth <= 0 || options.outHeight <= 0) return Outcome.Failure(AppError.InvalidImage)
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = context.contentResolver.openInputStream(if (uri.scheme == null) Uri.fromFile(java.io.File(uri.toString())) else uri)
            ?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return Outcome.Failure(AppError.InvalidImage)
        if (bitmap.width == 0 || bitmap.height == 0) return Outcome.Failure(AppError.InvalidImage)
        val orientation = context.contentResolver.openInputStream(if (uri.scheme == null) Uri.fromFile(java.io.File(uri.toString())) else uri)?.use {
            androidx.exifinterface.media.ExifInterface(it).getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(90f); postScale(1f, -1f) }
                8 -> setRotate(270f)
            }
        }
        val oriented = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
        ).also { if (it !== bitmap) bitmap.recycle() }
        val longest = maxOf(oriented.width, oriented.height)
        val bounded = if (longest <= maxEdge) oriented else {
            val ratio = maxEdge.toFloat() / longest
            Bitmap.createScaledBitmap(oriented, (oriented.width * ratio).toInt().coerceAtLeast(1),
                (oriented.height * ratio).toInt().coerceAtLeast(1), true).also {
                if (it !== oriented) oriented.recycle()
            }
        }
        Outcome.Success(bounded)
        } catch (oom: OutOfMemoryError) {
            Outcome.Failure(AppError.Memory)
        } catch (e: Exception) {
            Outcome.Failure(AppError.InvalidImage)
        }
    }

    /** Save exactly the rectangle shown over the oriented image; no hidden view-space crop. */
    fun crop(source: Bitmap, selection: FabricCrop): Bitmap {
        val rect = selection.pixels(source.width, source.height)
        return Bitmap.createBitmap(source, rect.x, rect.y, rect.width, rect.height)
    }

    fun downscaleForSave(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= SAVE_MAX_EDGE) return bitmap
        val ratio = SAVE_MAX_EDGE.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    fun save(bitmap: Bitmap, prefix: String): File {
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            downscaleForSave(bitmap).compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return file
    }

    fun shareUriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file, file.name)

    fun shareUriForPath(path: String): Uri = shareUriFor(File(path))

    fun cacheForShare(bitmap: Bitmap, name: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file, name)
    }
}
