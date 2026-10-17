package com.fabricfit.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fabricfit.app.media.BitmapAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.abs

/**
 * Image cropping helper for the Create-post flow.
 *
 * This used to delegate to uCrop (`com.github.yalantis:ucrop`), which is no
 * longer published to Maven Central — the artefact cannot be resolved by a
 * clean build. Cropping is done here with platform APIs instead: decode a
 * memory-bounded copy of the source, centre-crop it to the requested aspect
 * ratio, and write the result to the app cache as a JPEG.
 */
object ImageEditor {

    /**
     * Aspect ratio presets for cropping.
     */
    enum class AspectRatio(val label: String, val ratioX: Float, val ratioY: Float) {
        FREE("Free", 0f, 0f),
        SQUARE("1:1", 1f, 1f),
        PORTRAIT("4:5", 4f, 5f),
        LANDSCAPE("16:9", 16f, 9f),
        STORY("9:16", 9f, 16f),
    }

    /**
     * Crop [sourceUri] to [aspectRatio] and write the result to the cache.
     * Returns the cropped file's [Uri], or null when the image cannot be
     * decoded or written.
     */
    suspend fun cropToCache(
        context: Context,
        sourceUri: Uri,
        aspectRatio: AspectRatio = AspectRatio.FREE,
        maxWidth: Int = 1920,
        maxHeight: Int = 1920,
    ): Uri? = withContext(Dispatchers.IO) {
        val maxEdge = maxOf(maxWidth, maxHeight)
        val source = BitmapAnalyzer(context).decodeSampled(sourceUri, maxEdge)
            ?: return@withContext null
        try {
            val cropped = cropToRatio(source, aspectRatio)
            val bounded = boundTo(cropped, maxWidth, maxHeight)
            val destination = File(context.cacheDir, "cropped_${UUID.randomUUID()}.jpg")
            destination.outputStream().use { out ->
                bounded.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(destination)
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        } finally {
            if (!source.isRecycled) source.recycle()
        }
    }

    /** Centre-crop to the requested ratio; FREE keeps the whole frame. */
    private fun cropToRatio(source: Bitmap, ratio: AspectRatio): Bitmap {
        val target = ratio.ratioX / ratio.ratioY
        if (ratio == AspectRatio.FREE || target <= 0f) return source
        val current = source.width.toFloat() / source.height.toFloat()
        if (abs(current - target) < 0.01f) return source

        val cropWidth = if (current > target) (source.height * target).toInt() else source.width
        val cropHeight = if (current > target) source.height else (source.width / target).toInt()
        val width = cropWidth.coerceIn(1, source.width)
        val height = cropHeight.coerceIn(1, source.height)
        val left = ((source.width - width) / 2).coerceAtLeast(0)
        val top = ((source.height - height) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(source, left, top, width, height)
    }

    /** Only ever downscale — upscaling a crop just inflates the file. */
    private fun boundTo(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = minOf(
            maxWidth.toFloat() / source.width.toFloat(),
            maxHeight.toFloat() / source.height.toFloat(),
            1f,
        )
        if (scale >= 1f) return source
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }
}

/**
 * Composable state holder for image editing workflow.
 */
@Composable
fun rememberImageEditorState(): ImageEditorState {
    return remember { ImageEditorState() }
}

class ImageEditorState {
    var originalUri by mutableStateOf<Uri?>(null)
        private set
    var editedUri by mutableStateOf<Uri?>(null)
        private set
    var isEditing by mutableStateOf(false)
        private set
    var selectedAspectRatio by mutableStateOf(ImageEditor.AspectRatio.FREE)
        private set

    fun setOriginalUri(uri: Uri?) {
        originalUri = uri
        editedUri = null
    }

    fun setEditedUri(uri: Uri?) {
        editedUri = uri
        isEditing = false
    }

    fun startEditing() {
        isEditing = true
    }

    fun cancelEditing() {
        isEditing = false
    }

    fun setAspectRatio(ratio: ImageEditor.AspectRatio) {
        selectedAspectRatio = ratio
    }

    /**
     * Get the final image URI (edited if available, otherwise original).
     */
    fun getFinalUri(): Uri? = editedUri ?: originalUri
}
