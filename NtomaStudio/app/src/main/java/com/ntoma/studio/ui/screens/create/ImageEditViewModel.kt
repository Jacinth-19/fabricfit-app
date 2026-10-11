package com.ntoma.studio.ui.screens.create

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntoma.studio.di.AppContainer
import com.ntoma.studio.domain.model.AppError
import com.ntoma.studio.domain.model.Outcome
import com.ntoma.studio.media.FabricCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImageEditUiState(
    val bitmap: Bitmap? = null,
    val selection: FabricCrop = FabricCrop.CENTRE,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: AppError? = null,
    val resultPath: String? = null,
)

class ImageEditViewModel(private val container: AppContainer, private val uri: String, private val target: String) : ViewModel() {
    private val initialSelection = if (target == "person") FabricCrop.FULL else FabricCrop.CENTRE
    private val _state = MutableStateFlow(ImageEditUiState(selection = initialSelection))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                container.imageProcessor.load(Uri.parse(uri), com.ntoma.studio.media.ImageProcessor.EDIT_MAX_EDGE)
            }) {
                is Outcome.Success -> _state.value = _state.value.copy(bitmap = result.data, loading = false)
                is Outcome.Failure -> _state.value = _state.value.copy(error = result.error, loading = false)
            }
        }
    }

    fun select(crop: FabricCrop) {
        if (!_state.value.saving) _state.value = _state.value.copy(selection = crop)
    }
    fun reset() = select(initialSelection)
    fun rotate() {
        val s = _state.value
        val bitmap = s.bitmap ?: return
        if (s.saving) return
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f) }, true)
        _state.value = s.copy(bitmap = rotated, selection = initialSelection)
    }

    fun confirm() {
        val s = _state.value
        val bitmap = s.bitmap ?: return
        if (s.saving) return
        val rect = s.selection.pixels(bitmap.width, bitmap.height)
        if (rect.width < 24 || rect.height < 24) {
            _state.value = s.copy(error = AppError.InvalidImage)
            return
        }
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val cropped = container.imageProcessor.crop(bitmap, s.selection)
                    try { Outcome.Success(container.imageProcessor.save(cropped, if (target == "person") "person" else "fabric")) }
                    finally { if (cropped !== bitmap) cropped.recycle() }
                } catch (e: OutOfMemoryError) { Outcome.Failure(AppError.Memory) }
                catch (e: Exception) { Outcome.Failure(AppError.InvalidImage) }
            }
            _state.value = when (result) {
                is Outcome.Success -> _state.value.copy(saving = false, resultPath = result.data.absolutePath)
                is Outcome.Failure -> _state.value.copy(saving = false, error = result.error)
            }
        }
    }
}
