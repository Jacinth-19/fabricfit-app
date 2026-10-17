package com.fabricfit.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Wrapper composable that adds pull-to-refresh functionality to any scrollable content.
 * 
 * @param isRefreshing Whether the refresh is currently in progress
 * @param onRefresh Callback invoked when user pulls to refresh
 * @param listState The scroll state of the LazyColumn/LazyList
 * @param content The scrollable content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        content = { content() },
    )
}

/**
 * Check if the list is scrolled to the top (for pull-to-refresh).
 */
fun LazyListState.isScrolledToTop(): Boolean {
    return firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
}
