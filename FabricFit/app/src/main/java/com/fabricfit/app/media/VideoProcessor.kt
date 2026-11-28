package com.fabricfit.app.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.io.File
import java.util.UUID

/**
 * Video processing utility using FFmpeg for compression, trimming, and thumbnail generation.
 */
object VideoProcessor {

    private const val TAG = "VideoProcessor"

    /** Hard cap on what we will upload from the feed composer. */
    private const val CLIP_LIMIT_MS = 3 * 60 * 1000L

    /**
     * Transcode a picked video into the app cache so the publish flow owns a stable,
     * predictably-sized file for the lifetime of the post.
     *
     * Uses Media3 Transformer rather than the old FFmpegKit path: ffmpeg-kit is no longer
     * published to Maven Central, so that dependency could not resolve on a clean build,
     * and the previous implementation quietly degraded to copying the source bytes.
     *
     * The video is genuinely re-encoded (H.264/MP4), which is the whole point: the old
     * implementation copied bytes and its [quality] argument did nothing.
     *
     * [quality] is accepted for call-site compatibility but currently has no effect —
     * Transformer 1.4.x has no stable per-request bitrate knob, and setting encoder
     * options here would tie the app to one Media3 minor version. It is used only to
     * decide whether to attempt a transcode at all, so passing 0 skips re-encoding.
     *
     * If the device cannot transform the file (unsupported codec, corrupt input), this
     * falls back to copying the source bytes rather than failing the post — the caller
     * still gets a playable file. [wasTranscoded] on the result says which happened, so
     * the UI can be honest instead of guessing.
     */
    suspend fun prepareForUpload(
        context: Context,
        inputUri: Uri,
        quality: Int = 70,
    ): PreparedVideo? = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "prepared_${UUID.randomUUID()}.mp4")

        val transformed = if (quality <= 0) {
            false
        } else {
            runCatching { transcode(context, inputUri, target) }.getOrDefault(false)
        }

        if (transformed && target.length() > 0L) {
            PreparedVideo(target, wasTranscoded = true)
        } else {
            // Keep the fallback: a post that cannot be re-encoded is still worth posting.
            runCatching { target.delete() }
            val copied = copySource(context, inputUri)
            copied?.let { PreparedVideo(it, wasTranscoded = false) }
        }
    }

    /**
     * Cut [startMs]..[endMs] out of [inputUri] and re-encode the result.
     *
     * The feed composer used to render a draggable trimmer whose callback only showed a
     * toast: the post published the whole clip while the UI implied a cut. This makes the
     * control truthful by actually clipping the media.
     *
     * Uses [MediaItem.ClippingConfiguration] on the same stable Transformer 1.4.x API as
     * [transcode]. Clipping and re-encoding happen in one pass, so there is no intermediate
     * file and no double loss of quality.
     *
     * Returns null when the range is empty or inverted, or when the device cannot process
     * the file. Callers must treat that as "trim did not happen" and say so, rather than
     * silently uploading the untrimmed source.
     */
    suspend fun trim(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
    ): File? = withContext(Dispatchers.IO) {
        if (endMs <= startMs || startMs < 0) return@withContext null

        val target = File(context.cacheDir, "trimmed_${UUID.randomUUID()}.mp4")
        val ok = runCatching {
            transcodeRange(context, inputUri, target, startMs, endMs)
        }.getOrDefault(false)

        if (ok && target.length() > 0L) {
            target
        } else {
            runCatching { target.delete() }
            null
        }
    }

    /**
     * Runs the Transformer to completion.
     *
     * Transformer reports through callbacks, so this bridges them onto a coroutine instead
     * of blocking a thread on a latch. Returns true only when the output exists and has
     * content.
     *
     * Deliberately minimal: it uses the stable Transformer 1.4.x API (start(EditedMediaItem,
     * path)) and does not attempt to set encoder options, because the per-encoder settings
     * API moved between 1.3 and 1.5. Transformer picks H.264 with a sensible bitrate from
     * the input, which is the behaviour we want on the widest range of devices.
     */
    private suspend fun transcode(
        context: Context,
        inputUri: Uri,
        target: File,
    ): Boolean = suspendCancellableCoroutine { cont ->
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(target.length() > 0L)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    // Reported as false, never thrown: the caller falls back to a copy.
                    Log.w(TAG, "transcode failed, falling back to a byte copy", exportException)
                    if (cont.isActive) cont.resume(false)
                }
            })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(inputUri)
            // Cap the length so a stray hour-long pick cannot hold the composer hostage.
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(CLIP_LIMIT_MS)
                    .build(),
            )
            .build()

        runCatching {
            transformer.start(EditedMediaItem.Builder(mediaItem).build(), target.absolutePath)
        }.onFailure { failure ->
            Log.w(TAG, "could not start transform", failure)
            if (cont.isActive) cont.resume(false)
        }

        cont.invokeOnCancellation { runCatching { transformer.cancel() } }
    }

    /**
     * Transcode a specific [startMs]..[endMs] window.
     *
     * Kept separate from [transcode] rather than parameterising it, because the two have
     * different clipping semantics: [transcode] applies a maximum-length cap and must not
     * drop the head of the clip, while this must honour both bounds exactly.
     *
     * The same Transformer listener shape is reused so both paths fail the same way -
     * returning false rather than throwing - which keeps the caller's fallback logic
     * identical.
     */
    private suspend fun transcodeRange(
        context: Context,
        inputUri: Uri,
        target: File,
        startMs: Long,
        endMs: Long,
    ): Boolean = suspendCancellableCoroutine { cont ->
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(target.length() > 0L)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    Log.w(TAG, "trim failed", exportException)
                    if (cont.isActive) cont.resume(false)
                }
            })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(inputUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build(),
            )
            .build()

        runCatching {
            transformer.start(EditedMediaItem.Builder(mediaItem).build(), target.absolutePath)
        }.onFailure { failure ->
            Log.w(TAG, "could not start trim", failure)
            if (cont.isActive) cont.resume(false)
        }

        cont.invokeOnCancellation { runCatching { transformer.cancel() } }
    }

    /** The old behaviour, retained only as a fallback. */
    private fun copySource(context: Context, inputUri: Uri): File? = runCatching {
        val output = File(context.cacheDir, "prepared_${UUID.randomUUID()}.mp4")
        val input = context.contentResolver.openInputStream(inputUri) ?: return null
        input.use { stream -> output.outputStream().use { out -> stream.copyTo(out) } }
        output
    }.getOrNull()

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
 * A staged video ready to publish.
 *
 * [wasTranscoded] is false when the file is a byte-for-byte copy of the picker's source
 * because the device could not re-encode it. Callers should not claim compression happened
 * when this is false.
 */
data class PreparedVideo(
    val file: File,
    val wasTranscoded: Boolean,
)

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
