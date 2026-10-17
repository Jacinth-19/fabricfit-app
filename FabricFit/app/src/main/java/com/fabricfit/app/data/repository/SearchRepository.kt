package com.fabricfit.app.data.repository

import com.fabricfit.app.data.local.db.GalleryItemDao
import com.fabricfit.app.data.local.db.CommunityUserDao
import com.fabricfit.app.data.local.db.StyleChallengeDao
import com.fabricfit.app.data.local.db.RecentSearchDao
import com.fabricfit.app.data.mapper.toDomain
import com.fabricfit.app.domain.model.DateRange
import com.fabricfit.app.domain.model.GalleryItem
import com.fabricfit.app.domain.model.CommunityUser
import com.fabricfit.app.domain.model.RecentSearch
import com.fabricfit.app.domain.model.SearchFilters
import com.fabricfit.app.domain.model.SearchResult
import com.fabricfit.app.domain.model.SearchResultType
import com.fabricfit.app.domain.model.SortOption
import com.fabricfit.app.domain.model.StyleChallenge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Repository for advanced search functionality across posts, users, tags, and challenges.
 */
class SearchRepository(
    private val galleryDao: GalleryItemDao,
    private val userDao: CommunityUserDao,
    private val challengeDao: StyleChallengeDao,
    private val recentSearchDao: RecentSearchDao,
) {
    /**
     * Search across all entity types with filters.
     */
    suspend fun search(
        query: String,
        filters: SearchFilters = SearchFilters(),
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<SearchResult>()
        val lowerQuery = query.lowercase()

        // Search posts
        val posts = searchPosts(lowerQuery, filters)
        results.addAll(posts)

        // Search users
        val users = searchUsers(lowerQuery)
        results.addAll(users)

        // Search tags
        val tags = searchTags(lowerQuery)
        results.addAll(tags)

        // Search challenges
        val challenges = searchChallenges(lowerQuery)
        results.addAll(challenges)

        // Apply sorting
        return applySorting(results, filters.sortBy, query)
    }

    /**
     * Search posts with filters.
     */
    private suspend fun searchPosts(
        query: String,
        filters: SearchFilters,
    ): List<SearchResult> {
        val allPosts = galleryDao.getAll()
        
        return allPosts
            .filter { post ->
                // Text search
                val matchesQuery = post.title.lowercase().contains(query) ||
                    post.description.lowercase().contains(query) ||
                    post.tags.any { it.lowercase().contains(query) }

                // Filter by tags
                val matchesTags = filters.selectedTags.isEmpty() ||
                    post.tags.any { it in filters.selectedTags }

                // Filter by fabrics
                val matchesFabrics = filters.selectedFabrics.isEmpty() ||
                    post.fabricIds.any { it in filters.selectedFabrics }

                // Filter by date range
                val matchesDate = matchesDateRange(post.createdAt, filters.dateRange)

                // Filter by engagement
                val matchesLikes = post.likeCount >= filters.minLikes
                val matchesComments = post.commentCount >= filters.minComments

                matchesQuery && matchesTags && matchesFabrics && matchesDate && matchesLikes && matchesComments
            }
            .map { post ->
                SearchResult(
                    id = post.id,
                    type = SearchResultType.POST,
                    title = post.title,
                    subtitle = "${post.likeCount} likes • ${post.commentCount} comments",
                    imageUrl = post.imageUrl,
                    timestamp = post.createdAt,
                    metadata = mapOf(
                        "likeCount" to post.likeCount,
                        "commentCount" to post.commentCount,
                        "username" to post.username,
                    ),
                )
            }
    }

    /**
     * Search users by username or display name.
     */
    private suspend fun searchUsers(query: String): List<SearchResult> {
        val allUsers = userDao.getAll()
        
        return allUsers
            .filter { user ->
                user.username.lowercase().contains(query) ||
                    user.displayName.lowercase().contains(query)
            }
            .map { user ->
                SearchResult(
                    id = user.id,
                    type = SearchResultType.USER,
                    title = user.displayName,
                    subtitle = "@${user.username} • ${user.followerCount} followers",
                    imageUrl = user.avatarUrl,
                    timestamp = user.createdAt,
                    metadata = mapOf(
                        "username" to user.username,
                        "followerCount" to user.followerCount,
                    ),
                )
            }
    }

    /**
     * Search tags across all posts.
     */
    private suspend fun searchTags(query: String): List<SearchResult> {
        val allPosts = galleryDao.getAll()
        val allTags = allPosts.flatMap { it.tags }.distinct()
        
        return allTags
            .filter { it.lowercase().contains(query) }
            .map { tag ->
                val postCount = allPosts.count { tag in it.tags }
                SearchResult(
                    id = tag,
                    type = SearchResultType.TAG,
                    title = "#$tag",
                    subtitle = "$postCount posts",
                    timestamp = System.currentTimeMillis(),
                    metadata = mapOf("postCount" to postCount),
                )
            }
    }

    /**
     * Search challenges by title or description.
     */
    private suspend fun searchChallenges(query: String): List<SearchResult> {
        val allChallenges = challengeDao.getAll()
        
        return allChallenges
            .filter { challenge ->
                challenge.title.lowercase().contains(query) ||
                    challenge.description.lowercase().contains(query)
            }
            .map { challenge ->
                SearchResult(
                    id = challenge.id,
                    type = SearchResultType.CHALLENGE,
                    title = challenge.title,
                    subtitle = "${challenge.participantCount} participants",
                    imageUrl = challenge.bannerUrl,
                    timestamp = challenge.startDate,
                    metadata = mapOf(
                        "participantCount" to challenge.participantCount,
                        "isActive" to challenge.isActive,
                    ),
                )
            }
    }

    /**
     * Check if timestamp matches the date range filter.
     */
    private fun matchesDateRange(timestamp: Long, dateRange: DateRange): Boolean {
        if (dateRange == DateRange.ALL_TIME) return true

        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (dateRange) {
            DateRange.TODAY -> {
                calendar.timeInMillis = now
                val startOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                timestamp >= startOfDay
            }
            DateRange.THIS_WEEK -> {
                calendar.timeInMillis = now
                val startOfWeek = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                timestamp >= startOfWeek
            }
            DateRange.THIS_MONTH -> {
                calendar.timeInMillis = now
                val startOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                timestamp >= startOfMonth
            }
            DateRange.THIS_YEAR -> {
                calendar.timeInMillis = now
                val startOfYear = calendar.apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                timestamp >= startOfYear
            }
            DateRange.ALL_TIME -> true
        }
    }

    /**
     * Apply sorting to search results.
     */
    private fun applySorting(
        results: List<SearchResult>,
        sortBy: SortOption,
        query: String,
    ): List<SearchResult> {
        return when (sortBy) {
            SortOption.NEWEST -> results.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> results.sortedBy { it.timestamp }
            SortOption.MOST_LIKED -> results.sortedByDescending {
                (it.metadata["likeCount"] as? Int) ?: 0
            }
            SortOption.MOST_COMMENTED -> results.sortedByDescending {
                (it.metadata["commentCount"] as? Int) ?: 0
            }
            SortOption.MOST_RELEVANT -> {
                // Simple relevance: exact matches first, then partial matches
                results.sortedByDescending { result ->
                    val titleMatch = result.title.lowercase() == query
                    val startsWith = result.title.lowercase().startsWith(query)
                    when {
                        titleMatch -> 2
                        startsWith -> 1
                        else -> 0
                    }
                }
            }
        }
    }

    /**
     * Get recent searches.
     */
    fun observeRecentSearches(): Flow<List<RecentSearch>> {
        return recentSearchDao.observeRecent().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Save a search query to recent searches.
     */
    suspend fun saveRecentSearch(query: String) {
        if (query.isBlank()) return
        recentSearchDao.insert(
            com.fabricfit.app.data.local.db.RecentSearchEntity(
                query = query,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    /**
     * Delete a recent search.
     */
    suspend fun deleteRecentSearch(query: String) {
        recentSearchDao.delete(query)
    }

    /**
     * Clear all recent searches.
     */
    suspend fun clearRecentSearches() {
        recentSearchDao.deleteAll()
    }

    /**
     * Get all unique tags for filter suggestions.
     */
    suspend fun getAllTags(): List<String> {
        val allPosts = galleryDao.getAll()
        return allPosts.flatMap { it.tags }.distinct().sorted()
    }
}
