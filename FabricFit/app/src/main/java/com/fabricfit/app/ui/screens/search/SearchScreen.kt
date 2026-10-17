package com.fabricfit.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fabricfit.app.R
import com.fabricfit.app.domain.model.SearchResult
import com.fabricfit.app.domain.model.SearchResultType
import com.fabricfit.app.ui.components.SearchAutocomplete
import com.fabricfit.app.ui.components.SearchSuggestion
import com.fabricfit.app.ui.components.SearchSuggestionGenerator
import com.fabricfit.app.ui.components.SuggestionType
import com.fabricfit.app.ui.navigation.Routes
import com.fabricfit.app.ui.navigation.appViewModel
import kotlinx.coroutines.launch

/**
 * Advanced search screen with filtering, sorting, and multi-entity search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: SearchViewModel = appViewModel { c, _ -> SearchViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(padding)) {
        // Top bar with search
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.updateQuery(it) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.clearSearch() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )

            IconButton(onClick = { vm.toggleFilters() }) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.action_filter),
                )
            }
        }

        // Search autocomplete suggestions
        if (state.query.length >= 1) {
            val suggestions = remember(state.query, state.recentSearches) {
                SearchSuggestionGenerator.generateSuggestions(
                    query = state.query,
                    recentSearches = state.recentSearches.map { it.query },
                    popularTags = listOf("fashion", "style", "outfit", "design", "fabric"),
                    users = listOf("jane_designer", "mike_stylist", "sarah_fashion"),
                )
            }
            
            if (suggestions.isNotEmpty()) {
                SearchAutocomplete(
                    suggestions = suggestions,
                    onSuggestionClick = { suggestion ->
                        when (suggestion.type) {
                            SuggestionType.RECENT_SEARCH -> {
                                vm.updateQuery(suggestion.text)
                                vm.performSearch()
                            }
                            SuggestionType.TAG -> {
                                val tagName = suggestion.text.removePrefix("#")
                                vm.updateQuery("#$tagName")
                                vm.performSearch()
                            }
                            SuggestionType.USER -> {
                                val username = suggestion.text.removePrefix("@")
                                vm.updateQuery("@$username")
                                vm.performSearch()
                            }
                            SuggestionType.POST -> {
                                vm.updateQuery(suggestion.text)
                                vm.performSearch()
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // Filter chips row
        if (state.filters.selectedTags.isNotEmpty() || 
            state.filters.dateRange != com.fabricfit.app.domain.model.DateRange.ALL_TIME ||
            state.filters.sortBy != com.fabricfit.app.domain.model.SortOption.NEWEST) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.filters.dateRange != com.fabricfit.app.domain.model.DateRange.ALL_TIME) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { vm.setDateRange(com.fabricfit.app.domain.model.DateRange.ALL_TIME) },
                            label = { Text(state.filters.dateRange.name) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )
                    }
                }
                items(state.filters.selectedTags) { tag ->
                    FilterChip(
                        selected = true,
                        onClick = { vm.toggleTag(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
        }

        // Content
        when {
            state.query.isEmpty() -> {
                // Show recent searches
                RecentSearchesSection(
                    recentSearches = state.recentSearches,
                    onSearchClick = { vm.useRecentSearch(it) },
                    onDeleteClick = { vm.deleteRecentSearch(it) },
                    onClearAllClick = { vm.clearRecentSearches() },
                )
            }
            state.isSearching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                // Show results with tabs
                Column(Modifier.fillMaxSize()) {
                    // Tabs
                    val tabs = listOf(
                        null to "All (${state.results.size})",
                        SearchResultType.POST to "Posts (${state.filteredResults[SearchResultType.POST]?.size ?: 0})",
                        SearchResultType.USER to "Users (${state.filteredResults[SearchResultType.USER]?.size ?: 0})",
                        SearchResultType.TAG to "Tags (${state.filteredResults[SearchResultType.TAG]?.size ?: 0})",
                        SearchResultType.CHALLENGE to "Challenges (${state.filteredResults[SearchResultType.CHALLENGE]?.size ?: 0})",
                    )

                    ScrollableTabRow(
                        selectedTabIndex = tabs.indexOfFirst { it.first == state.selectedTab }.coerceAtLeast(0),
                        edgePadding = 16.dp,
                    ) {
                        tabs.forEachIndexed { index, (type, label) ->
                            Tab(
                                selected = state.selectedTab == type,
                                onClick = { vm.selectTab(type) },
                                text = { Text(label) },
                            )
                        }
                    }

                    // Results list
                    val displayResults = if (state.selectedTab == null) {
                        state.results
                    } else {
                        state.filteredResults[state.selectedTab] ?: emptyList()
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(displayResults, key = { "${it.type}_${it.id}" }) { result ->
                            SearchResultCard(
                                result = result,
                                onClick = {
                                    when (result.type) {
                                        SearchResultType.POST -> nav.navigate(Routes.galleryDetail(result.id))
                                        SearchResultType.USER -> nav.navigate(Routes.userProfile(result.id))
                                        SearchResultType.TAG -> {
                                            vm.updateQuery(result.title.removePrefix("#"))
                                            vm.performSearch()
                                        }
                                        SearchResultType.CHALLENGE -> nav.navigate(Routes.challenge(result.id))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (state.showFilters) {
        ModalBottomSheet(
            onDismissRequest = { vm.toggleFilters() },
            sheetState = sheetState,
        ) {
            FilterPanel(
                filters = state.filters,
                allTags = state.allTags,
                onDateRangeChange = { vm.setDateRange(it) },
                onSortChange = { vm.setSortOption(it) },
                onTagToggle = { vm.toggleTag(it) },
                onMinLikesChange = { vm.setMinLikes(it) },
                onMinCommentsChange = { vm.setMinComments(it) },
                onClearFilters = { vm.clearFilters() },
                onApply = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) vm.toggleFilters()
                    }
                },
            )
        }
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<com.fabricfit.app.domain.model.RecentSearch>,
    onSearchClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onClearAllClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.search_recent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (recentSearches.isNotEmpty()) {
                TextButton(onClick = onClearAllClick) {
                    Text(stringResource(R.string.action_clear_all))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (recentSearches.isEmpty()) {
            Text(
                stringResource(R.string.search_no_recent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(recentSearches, key = { it.query }) { search ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSearchClick(search.query) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            search.query,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onDeleteClick(search.query) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Image/Avatar
            if (result.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(result.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(
                            if (result.type == SearchResultType.USER) CircleShape 
                            else RoundedCornerShape(8.dp)
                        ),
                )
                Spacer(Modifier.width(12.dp))
            }

            // Content
            Column(Modifier.weight(1f)) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (result.subtitle.isNotEmpty()) {
                    Text(
                        result.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Type badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    when (result.type) {
                        SearchResultType.POST -> "POST"
                        SearchResultType.USER -> "USER"
                        SearchResultType.TAG -> "TAG"
                        SearchResultType.CHALLENGE -> "EVENT"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun FilterPanel(
    filters: com.fabricfit.app.domain.model.SearchFilters,
    allTags: List<String>,
    onDateRangeChange: (com.fabricfit.app.domain.model.DateRange) -> Unit,
    onSortChange: (com.fabricfit.app.domain.model.SortOption) -> Unit,
    onTagToggle: (String) -> Unit,
    onMinLikesChange: (Int) -> Unit,
    onMinCommentsChange: (Int) -> Unit,
    onClearFilters: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.filter_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(16.dp))

        // Date range
        Text(
            stringResource(R.string.filter_date_range),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(com.fabricfit.app.domain.model.DateRange.values().toList()) { range ->
                FilterChip(
                    selected = filters.dateRange == range,
                    onClick = { onDateRangeChange(range) },
                    label = { Text(range.name.replace("_", " ")) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sort by
        Text(
            stringResource(R.string.filter_sort_by),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(com.fabricfit.app.domain.model.SortOption.values().toList()) { option ->
                FilterChip(
                    selected = filters.sortBy == option,
                    onClick = { onSortChange(option) },
                    label = { Text(option.name.replace("_", " ")) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tags
        Text(
            stringResource(R.string.filter_tags),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allTags.take(20)) { tag ->
                FilterChip(
                    selected = tag in filters.selectedTags,
                    onClick = { onTagToggle(tag) },
                    label = { Text("#$tag") },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_clear))
            }
            androidx.compose.material3.Button(
                onClick = onApply,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_apply))
            }
        }
    }
}
