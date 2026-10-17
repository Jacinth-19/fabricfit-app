package com.fabricfit.app.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.GalleryItem
import com.fabricfit.app.domain.model.StyleChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommunityUiState(
    val galleryItems: List<GalleryItem> = emptyList(),
    val activeChallenges: List<StyleChallenge> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class CommunityViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    init {
        observeGallery()
        observeChallenges()
    }

    private fun observeGallery() {
        viewModelScope.launch {
            container.community.observeAllGalleryItems().collect { items ->
                _state.value = _state.value.copy(
                    galleryItems = items,
                    isLoading = false
                )
            }
        }
    }

    private fun observeChallenges() {
        viewModelScope.launch {
            container.community.observeActiveChallenges().collect { challenges ->
                _state.value = _state.value.copy(activeChallenges = challenges)
            }
        }
    }

    fun toggleLike(itemId: String) {
        viewModelScope.launch {
            try {
                container.community.toggleLike(itemId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to update like: ${e.message}")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
