package com.fabricfit.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.DateRange
import com.fabricfit.app.domain.model.RecentSearch
import com.fabricfit.app.domain.model.SearchFilters
import com.fabricfit.app.domain.model.SearchResult
import com.fabricfit.app.domain.model.SearchResultType
import com.fabricfit.app.domain.model.SortOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the search screen.
 */
data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val filteredResults: Map<SearchResultType, List<SearchResult>> = emptyMap(),
    val recentSearches: List<RecentSearch> = emptyList(),
    val allTags: List<String> = emptyList(),
    val filters: SearchFilters = SearchFilters(),
    val selectedTab: SearchResultType? = null,
    val showFilters: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for the search screen with advanced filtering and sorting.
 */
class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRecentSearches()
        loadAllTags()
    }

    /**
     * Load recent searches from database.
     */
    private fun loadRecentSearches() {
        viewModelScope.launch {
            container.search.observeRecentSearches().collect { searches ->
                _state.value = _state.value.copy(recentSearches = searches)
            }
        }
    }

    /**
     * Load all available tags for filter suggestions.
     */
    private fun loadAllTags() {
        viewModelScope.launch {
            val tags = container.search.getAllTags()
            _state.value = _state.value.copy(allTags = tags)
        }
    }

    /**
     * Update search query with debouncing.
     */
    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
        
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300) // Debounce
                performSearch()
            }
        } else {
            _state.value = _state.value.copy(
                results = emptyList(),
                filteredResults = emptyMap(),
            )
        }
    }

    /**
     * Perform search with current query and filters.
     */
    fun performSearch() {
        val query = _state.value.query
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, error = null)

            try {
                // Save to recent searches
                container.search.saveRecentSearch(query)

                // Perform search
                val results = container.search.search(query, _state.value.filters)
                
                // Group results by type
                val grouped = results.groupBy { it.type }

                _state.value = _state.value.copy(
                    isSearching = false,
                    results = results,
                    filteredResults = grouped,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSearching = false,
                    error = "Search failed: ${e.message}",
                )
            }
        }
    }

    /**
     * Update search filters.
     */
    fun updateFilters(filters: SearchFilters) {
        _state.value = _state.value.copy(filters = filters)
        if (_state.value.query.isNotBlank()) {
            performSearch()
        }
    }

    /**
     * Update date range filter.
     */
    fun setDateRange(dateRange: DateRange) {
        updateFilters(_state.value.filters.copy(dateRange = dateRange))
    }

    /**
     * Update sort option.
     */
    fun setSortOption(sortOption: SortOption) {
        updateFilters(_state.value.filters.copy(sortBy = sortOption))
    }

    /**
     * Toggle tag filter.
     */
    fun toggleTag(tag: String) {
        val currentTags = _state.value.filters.selectedTags
        val newTags = if (tag in currentTags) {
            currentTags - tag
        } else {
            currentTags + tag
        }
        updateFilters(_state.value.filters.copy(selectedTags = newTags))
    }

    /**
     * Update minimum likes filter.
     */
    fun setMinLikes(minLikes: Int) {
        updateFilters(_state.value.filters.copy(minLikes = minLikes))
    }

    /**
     * Update minimum comments filter.
     */
    fun setMinComments(minComments: Int) {
        updateFilters(_state.value.filters.copy(minComments = minComments))
    }

    /**
     * Clear all filters.
     */
    fun clearFilters() {
        updateFilters(SearchFilters())
    }

    /**
     * Select a result tab.
     */
    fun selectTab(tab: SearchResultType?) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    /**
     * Toggle filter panel visibility.
     */
    fun toggleFilters() {
        _state.value = _state.value.copy(showFilters = !_state.value.showFilters)
    }

    /**
     * Delete a recent search.
     */
    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            container.search.deleteRecentSearch(query)
        }
    }

    /**
     * Clear all recent searches.
     */
    fun clearRecentSearches() {
        viewModelScope.launch {
            container.search.clearRecentSearches()
        }
    }

    /**
     * Use a recent search.
     */
    fun useRecentSearch(query: String) {
        updateQuery(query)
        performSearch()
    }

    /**
     * Clear search.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _state.value = SearchUiState(
            recentSearches = _state.value.recentSearches,
            allTags = _state.value.allTags,
        )
    }

    /**
     * Clear error.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
