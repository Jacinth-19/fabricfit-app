package com.fabricfit.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import com.fabricfit.app.domain.model.AppError
import com.fabricfit.app.domain.model.Outcome
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
        val type = context.contentResolver.getType(uri)
        if (type != null && !type.startsWith("image/")) return Outcome.Failure(AppError.UnsupportedImage)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            // decodeStream returns null when it only reads the bounds, so the stream is the
            // thing whose absence means "unreadable" - not the return value.
            val stream = openStream(uri) ?: return Outcome.Failure(AppError.InvalidImage)
            stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        } catch (e: Exception) {
            return Outcome.Failure(AppError.InvalidImage)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return Outcome.Failure(AppError.InvalidImage)
        if (bounds.outWidth.toLong() * bounds.outHeight > MAX_PIXELS * 4) return Outcome.Failure(AppError.ImageTooLarge)
        return Outcome.Success(uri)
    }

    /**
     * Opens an image stream for a Uri.
     *
     * App-private files (`file://` or a bare path, e.g. the bundled demo model, saved looks
     * and cached shares) do not go through the ContentResolver, which throws
     * `FileNotFoundException` for them; read those straight from disk instead.
     */
    private fun openStream(uri: Uri): java.io.InputStream? {
        val scheme = uri.scheme
        if (scheme == null || scheme == "file") {
            // A bare path parses to a Uri whose `toString()` is the path itself, while
            // `file:///path` exposes it as `path`. Try both spellings and take the one
            // that is actually on disk.
            val candidates = listOfNotNull(uri.path, uri.toString()).filter { it.isNotBlank() }
            for (path in candidates) {
                val file = File(path)
                if (file.exists() && file.isFile) return file.inputStream()
            }
            return null
        }
        return context.contentResolver.openInputStream(uri)
    }

    /**
     * Loads an image from a filesystem path or a `file://` uri string.
     *
     * The FitRoom passes the sample model and saved looks around as plain paths, which
     * `Uri.parse` handles inconsistently across API levels, so path input bypasses the
     * Uri round-trip entirely.
     */
    fun load(path: String, maxEdge: Int): Outcome<Bitmap> {
        val file = File(path.removePrefix("file://"))
        if (file.exists() && file.isFile) return loadFile(file, maxEdge)
        return load(Uri.parse(path), maxEdge)
    }

    fun load(uri: Uri, maxEdge: Int): Outcome<Bitmap> =
        decode(maxEdge) { openStream(uri) }

    private fun loadFile(file: File, maxEdge: Int): Outcome<Bitmap> =
        decode(maxEdge) { file.inputStream() }

    /**
     * Two-pass decode: read the bounds, pick a power-of-two sample size, then decode.
     * `open` is called once per pass so the stream is never read twice.
     */
    private fun decode(maxEdge: Int, open: () -> java.io.InputStream?): Outcome<Bitmap> {
        return try {
            // Pass 1 reads only the bounds. decodeStream deliberately returns null for a
            // bounds-only decode, so the null check belongs on the stream, not the result.
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = open() ?: return Outcome.Failure(AppError.InvalidImage)
            boundsStream.use { BitmapFactory.decodeStream(it, null, options) }
            if (options.outWidth <= 0 || options.outHeight <= 0) return Outcome.Failure(AppError.InvalidImage)
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / (sample * 2) >= maxEdge) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val pixels = open() ?: return Outcome.Failure(AppError.InvalidImage)
            val bitmap = pixels.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return Outcome.Failure(AppError.InvalidImage)
            if (bitmap.width == 0 || bitmap.height == 0) return Outcome.Failure(AppError.InvalidImage)
            Outcome.Success(bitmap)
        } catch (oom: OutOfMemoryError) {
            Outcome.Failure(AppError.Memory)
        } catch (e: Exception) {
            Outcome.Failure(AppError.InvalidImage)
        }
    }

    /**
     * Applies rotation / scale / pan + aspect crop in view space, mirroring what the user saw
     * in the editor overlay.
     */
    fun transform(
        source: Bitmap,
        rotationDeg: Int,
        scale: Float,
        panX: Float,
        panY: Float,
        viewW: Int,
        viewH: Int,
        coverScale: Float,
    ): Bitmap {
        val matrix = Matrix()
        // Work in the view's coordinate space: centre the source, then undo the user transforms.
        matrix.postTranslate(source.width / -2f, source.height / -2f)
        matrix.postScale(1f / (coverScale * scale), 1f / (coverScale * scale))
        matrix.postRotate(-rotationDeg.toFloat())
        matrix.postTranslate(-panX, -panY)
        matrix.postTranslate(viewW / 2f, viewH / 2f)
        // The matrix above maps source->view; invert to sample source from view pixels.
        val inverse = Matrix()
        matrix.invert(inverse)

        val out = Bitmap.createBitmap(viewW, viewH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.setMatrix(inverse)
        canvas.drawBitmap(source, 0f, 0f, null)
        return out
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
