package com.fabricfit.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.StylePreference
import com.fabricfit.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileUiState(
    val prefs: UserPreferences = UserPreferences(),
    val fabricCount: Int = 0,
    val designCount: Int = 0,
    val lookCount: Int = 0,
    val favoriteCount: Int = 0,
)

class ProfileViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settings.preferences.collect { prefs ->
                _state.value = _state.value.copy(prefs = prefs)
            }
        }
        viewModelScope.launch {
            container.fabricAnalysis.observeAll().collect { list ->
                _state.value = _state.value.copy(fabricCount = list.size)
            }
        }
        viewModelScope.launch {
            container.dressStyles.observeAll().collect { list ->
                _state.value = _state.value.copy(designCount = list.size)
            }
        }
        viewModelScope.launch {
            container.tryOn.observeLooks().collect { list ->
                _state.value = _state.value.copy(lookCount = list.size)
            }
        }
        viewModelScope.launch {
            combine(
                container.fabricAnalysis.observeFavorites(),
                container.favorites.observeFavoriteStyleIds(),
                container.tryOn.observeFavoriteLooks(),
            ) { favFabrics, favStyles, favLooks ->
                favFabrics.size + favStyles.size + favLooks.size
            }.collect { totalFavs ->
                _state.value = _state.value.copy(favoriteCount = totalFavs)
            }
        }
    }

    fun setName(name: String) = viewModelScope.launch {
        container.settings.update { it.copy(displayName = name.trim()) }
    }

    fun updateProfile(
        name: String,
        bio: String,
        avatarUri: String,
        stylePreference: StylePreference,
    ) = viewModelScope.launch {
        container.settings.update {
            it.copy(
                displayName = name.trim(),
                profileBio = bio.trim(),
                avatarUri = avatarUri,
                stylePreference = stylePreference,
            )
        }
    }
}
