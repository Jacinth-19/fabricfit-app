package com.fabricfit.app.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Video processing utility using FFmpeg for compression, trimming, and thumbnail generation.
 */
object VideoProcessor {

    /**
     * Compress video to reduce file size.
     * @param context Application context
     * @param inputUri Source video URI
     * @param quality Compression quality (0-100, default 70)
     * @return Compressed video file, or null if failed
     */
    suspend fun compressVideo(
        context: Context,
        inputUri: Uri,
        quality: Int = 70,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(inputUri.path ?: return@withContext null)
            val outputFile = File(
                context.cacheDir,
                "compressed_${UUID.randomUUID()}.mp4"
            )

            // FFmpeg command for video compression
            // -c:v libx264: Use H.264 codec
            // -crf: Constant Rate Factor (lower = better quality, 23 is default)
            // -preset: Encoding speed (medium is balanced)
            // -c:a aac: Audio codec
            val crf = 23 + ((100 - quality) * 0.3).toInt() // Map 0-100 to CRF 23-53
            val command = "-i ${inputFile.absolutePath} " +
                "-c:v libx264 -crf $crf -preset medium " +
                "-c:a aac -b:a 128k " +
                "-movflags +faststart " +
                outputFile.absolutePath

            val session = FFmpegKit.execute(command)
            
            if (ReturnCode.isSuccess(session.returnCode)) {
                outputFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Trim video to specified duration.
     * @param context Application context
     * @param inputUri Source video URI
     * @param startTimeMs Start time in milliseconds
     * @param endTimeMs End time in milliseconds
     * @return Trimmed video file, or null if failed
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        startTimeMs: Long,
        endTimeMs: Long,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val inputFile = File(inputUri.path ?: return@withContext null)
            val outputFile = File(
                context.cacheDir,
                "trimmed_${UUID.randomUUID()}.mp4"
            )

            val startSeconds = startTimeMs / 1000.0
            val durationSeconds = (endTimeMs - startTimeMs) / 1000.0

            // FFmpeg command for trimming
            // -ss: Start time
            // -t: Duration
            // -c copy: Stream copy (fast, no re-encoding)
            val command = "-ss $startSeconds -i ${inputFile.absolutePath} " +
                "-t $durationSeconds " +
                "-c copy " +
                outputFile.absolutePath

            val session = FFmpegKit.execute(command)
            
            if (ReturnCode.isSuccess(session.returnCode)) {
                outputFile
            } else {
                null
            }
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
