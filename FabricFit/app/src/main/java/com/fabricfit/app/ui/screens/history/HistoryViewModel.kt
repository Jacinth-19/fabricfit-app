package com.fabricfit.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.HistoryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HistoryUiState(
    val events: List<HistoryEvent> = emptyList(),
    val isLoading: Boolean = true,
)

class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.history.observeAll().collect { events ->
                _state.value = HistoryUiState(events = events, isLoading = false)
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch { container.history.delete(id) }
    fun clear() = viewModelScope.launch { container.history.clear() }

    /** One-shot re-read of the history list; used by pull-to-refresh. */
    fun refresh(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value = HistoryUiState(container.history.observeAll().first())
            onComplete()
        }
    }
}
