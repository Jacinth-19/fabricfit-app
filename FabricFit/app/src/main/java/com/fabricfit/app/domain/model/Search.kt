package com.fabricfit.app.domain.model

/**
 * Search result types for the search screen.
 */
enum class SearchResultType {
    POST,
    USER,
    TAG,
    CHALLENGE
}

/**
 * Unified search result that can represent different entity types.
 */
data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap(),
)

/**
 * Search filters for advanced filtering.
 */
data class SearchFilters(
    val dateRange: DateRange = DateRange.ALL_TIME,
    val sortBy: SortOption = SortOption.NEWEST,
    val selectedTags: List<String> = emptyList(),
    val selectedFabrics: List<Long> = emptyList(),
    val selectedUsers: List<String> = emptyList(),
    val minLikes: Int = 0,
    val minComments: Int = 0,
)

enum class DateRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
}

enum class SortOption {
    NEWEST,
    OLDEST,
    MOST_LIKED,
    MOST_COMMENTED,
    MOST_RELEVANT
}

/**
 * Recent search query stored locally.
 */
data class RecentSearch(
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
)
