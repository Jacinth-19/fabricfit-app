package com.fabricfit.app.ui.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

/**
 * Image editor helper using uCrop library for cropping, rotating, and aspect ratio adjustments.
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
     * Launch uCrop activity for image editing.
     */
    fun launchEditor(
        activity: Activity,
        sourceUri: Uri,
        aspectRatio: AspectRatio = AspectRatio.FREE,
        maxWidth: Int = 1920,
        maxHeight: Int = 1920,
    ) {
        val destinationUri = Uri.fromFile(
            File(activity.cacheDir, "cropped_${UUID.randomUUID()}.jpg")
        )

        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withMaxResultSize(maxWidth, maxHeight)

        // Apply aspect ratio
        if (aspectRatio != AspectRatio.FREE) {
            uCrop.withAspectRatio(aspectRatio.ratioX, aspectRatio.ratioY)
        }

        // Configure uCrop options
        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setHideBottomControls(false) // Show crop controls
            setFreeStyleCropEnabled(aspectRatio == AspectRatio.FREE)
            
            // UI customization
            setToolbarColor(android.graphics.Color.parseColor("#1B4D3E")) // Heritage Forest
            setStatusBarColor(android.graphics.Color.parseColor("#1B4D3E"))
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#D4AF37")) // Heritage Gold
            setToolbarWidgetColor(android.graphics.Color.WHITE)
        }

        uCrop.withOptions(options)
            .start(activity)
    }

    /**
     * Parse uCrop result from activity result.
     */
    fun parseResult(resultCode: Int, data: Intent?): Uri? {
        if (resultCode == Activity.RESULT_OK && data != null) {
            return UCrop.getOutput(data)
        }
        return null
    }

    /**
     * Check if uCrop error occurred.
     */
    fun getError(data: Intent?): Throwable? {
        return UCrop.getError(data)
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
