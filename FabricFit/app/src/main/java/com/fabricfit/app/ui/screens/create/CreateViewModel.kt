package com.fabricfit.app.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.Fabric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateUiState(val recent: List<Fabric> = emptyList())

class CreateViewModel(container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.fabricAnalysis.observeAll().collect { fabrics ->
                _state.value = CreateUiState(recent = fabrics.take(4))
            }
        }
    }
}
