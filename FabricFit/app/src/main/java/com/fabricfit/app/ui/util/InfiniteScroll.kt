package com.fabricfit.app.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Triggers [onLoadMore] when the user scrolls near the end of the list.
 * 
 * @param listState The LazyListState to observe
 * @param buffer Number of items before the end to trigger loading (default: 3)
 * @param onLoadMore Callback to load more items
 */
@Composable
fun InfiniteScrollHandler(
    listState: LazyListState,
    buffer: Int = 3,
    onLoadMore: () -> Unit,
) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && 
                lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 1 - buffer
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onLoadMore()
            }
    }
}

/**
 * Pagination state for infinite scroll lists.
 */
data class PaginationState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val currentPage: Int = 0,
) {
    fun addItems(newItems: List<T>): PaginationState<T> {
        return copy(
            items = items + newItems,
            isLoading = false,
            hasMore = newItems.isNotEmpty(),
            currentPage = currentPage + 1,
        )
    }
    
    fun setLoading(): PaginationState<T> {
        return copy(isLoading = true, error = null)
    }
    
    fun setError(message: String): PaginationState<T> {
        return copy(isLoading = false, error = message)
    }
    
    fun reset(): PaginationState<T> {
        return PaginationState()
    }
}

/**
 * Prefetching configuration for smooth scrolling.
 */
data class PrefetchConfig(
    val prefetchDistance: Int = 5,
    val pageSize: Int = 20,
    val enablePrefetch: Boolean = true,
)

/**
 * Advanced infinite scroll with prefetching and error handling.
 */
@Composable
fun <T> AdvancedInfiniteScroll(
    state: PaginationState<T>,
    listState: LazyListState,
    config: PrefetchConfig = PrefetchConfig(),
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable (T, Int) -> Unit,
    loadingContent: @Composable () -> Unit = { DefaultLoadingItem() },
    errorContent: @Composable (String) -> Unit = { DefaultErrorItem(it, onRetry) },
) {
    InfiniteScrollHandler(
        listState = listState,
        buffer = config.prefetchDistance,
    ) {
        if (!state.isLoading && state.hasMore) {
            onLoadMore()
        }
    }
}

@Composable
private fun DefaultLoadingItem() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun DefaultErrorItem(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
