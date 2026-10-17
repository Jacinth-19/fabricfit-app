package com.fabricfit.app.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Example usage of PullToRefreshWrapper with a LazyColumn.
 * 
 * Usage:
 * ```
 * var isRefreshing by remember { mutableStateOf(false) }
 * var items by remember { mutableStateOf(listOf("Item 1", "Item 2")) }
 * 
 * PullToRefreshExample(
 *     items = items,
 *     onRefresh = {
 *         isRefreshing = true
 *         // Fetch new data
 *         delay(2000)
 *         items = fetchNewItems()
 *         isRefreshing = false
 *     }
 * )
 * ```
 */
@Composable
fun PullToRefreshExample(
    items: List<String>,
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            // Simulate network call
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                onRefresh()
                isRefreshing = false
            }
        },
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
        ) {
            items(items) { item ->
                Text(text = item)
            }
        }
    }
}
