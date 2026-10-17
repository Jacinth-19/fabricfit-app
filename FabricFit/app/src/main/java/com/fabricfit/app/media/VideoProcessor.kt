package com.fabricfit.app.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Video processing utility using FFmpeg for compression, trimming, and thumbnail generation.
 */
object VideoProcessor {

    /**
     * Copy a picked video into the app cache so the publish flow owns a stable
     * file for the lifetime of the post.
     *
     * NOTE: transcoding used to run through FFmpegKit (`com.arthenica:ffmpeg-kit`),
     * which is no longer published to Maven Central and cannot be resolved by a
     * clean build. Until the app moves to `androidx.media3` Transformer, this
     * copies the source bytes rather than re-encoding them, so [quality] is
     * accepted for call-site compatibility but has no effect yet.
     *
     * @param context Application context
     * @param inputUri Source video URI
     * @param quality Reserved for the future transcode pass (0-100, default 70)
     * @return Cached video file, or null if it could not be read
     */
    suspend fun prepareForUpload(
        context: Context,
        inputUri: Uri,
        quality: Int = 70,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(
                context.cacheDir,
                "prepared_${UUID.randomUUID()}.mp4"
            )
            val input = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext null
            input.use { stream ->
                outputFile.outputStream().use { out -> stream.copyTo(out) }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate thumbnail from video at specified time.
     * @param context Application context
     * @param videoUri Video URI
     * @param timeMs Time in milliseconds to capture thumbnail (default 0)
     * @return Thumbnail bitmap, or null if failed
     */
    suspend fun generateThumbnail(
        context: Context,
        videoUri: Uri,
        timeMs: Long = 0,
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get video duration in milliseconds.
     * @param context Application context
     * @param videoUri Video URI
     * @return Duration in milliseconds, or null if failed
     */
    suspend fun getVideoDuration(
        context: Context,
        videoUri: Uri,
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get video metadata (width, height, duration, bitrate).
     * @param context Application context
     * @param videoUri Video URI
     * @return VideoMetadata object, or null if failed
     */
    suspend fun getVideoMetadata(
        context: Context,
        videoUri: Uri,
    ): VideoMetadata? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()

            retriever.release()

            VideoMetadata(
                width = width ?: 0,
                height = height ?: 0,
                durationMs = duration ?: 0,
                bitrateKbps = (bitrate ?: 0) / 1000,
                rotation = rotation ?: 0,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save bitmap to file.
     * @param context Application context
     * @param bitmap Bitmap to save
     * @param fileName Output file name
     * @return Saved file, or null if failed
     */
    suspend fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "thumbnail_${UUID.randomUUID()}.jpg",
    ): File? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, fileName)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Video metadata information.
 */
data class VideoMetadata(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val bitrateKbps: Long,
    val rotation: Int,
)
