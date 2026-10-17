package com.fabricfit.app.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.domain.model.HistoryEvent
import com.fabricfit.app.ui.components.EnhancedEmptyState
import com.fabricfit.app.ui.components.ShimmerGrid
import com.fabricfit.app.ui.util.rememberHapticFeedback
import com.fabricfit.app.ui.components.LocalImage
import com.fabricfit.app.ui.navigation.appViewModel
import com.fabricfit.app.ui.util.resolveKey
import com.fabricfit.app.ui.util.shareImagePath
import com.fabricfit.app.ui.util.timeAgo
import kotlinx.coroutines.launch

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(nav: NavHostController, padding: PaddingValues) {
    val vm: HistoryViewModel = appViewModel { c, ctx -> HistoryViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var expandedMenuId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(padding)) {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                vm.refresh { refreshing = false }
            },
            modifier = Modifier.weight(1f),
        ) {
        Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.history_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.events.isNotEmpty()) {
                TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.history_clear)) }
            }
        }

        if (state.isLoading) {
            ShimmerGrid(
                columns = 1,
                rows = 5,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            )
        } else if (state.events.isEmpty()) {
            EnhancedEmptyState(
                illustration = R.drawable.illust_empty_activity,
                title = R.string.history_empty_title,
                body = R.string.history_empty_body,
                actionLabel = R.string.empty_action_scan_fabric,
                onAction = { nav.navigate(com.fabricfit.app.ui.navigation.Routes.CREATE) },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(state.events, key = { it.id }) { event ->
                    val haptics = rememberHapticFeedback()
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                haptics.heavy()
                                vm.delete(event.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_item),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.action_delete),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .combinedClickable(
                                        onClick = { /* Navigate to detail if needed */ },
                                        onLongClick = {
                                            haptics.tap()
                                            expandedMenuId = event.id
                                        }
                                    ),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (event.imageUri != null) {
                                        LocalImage(
                                            event.imageUri!!,
                                            contentDescription = null,
                                            modifier = Modifier.size(52.dp),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            labelFor(context, event),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            context.timeAgo(event.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = {
                                        haptics.tap()
                                        vm.delete(event.id)
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_item))
                                    }
                                }
                            }
                            
                            DropdownMenu(
                                expanded = expandedMenuId == event.id,
                                onDismissRequest = { expandedMenuId = null }
                            ) {
                                if (event.imageUri != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_share)) },
                                        onClick = {
                                            expandedMenuId = null
                                            context.shareImagePath(
                                                event.imageUri!!,
                                                labelFor(context, event)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Share, contentDescription = null)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete)) },
                                    onClick = {
                                        expandedMenuId = null
                                        haptics.heavy()
                                        vm.delete(event.id)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Delete, contentDescription = null)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
        }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch { vm.clear() }
                }) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun labelFor(context: android.content.Context, event: HistoryEvent): String {
    val subject = context.resolveKey(event.labelKey) ?: event.label.orEmpty()
    return when (event.kind) {
        HistoryEvent.Kind.SCAN -> context.getString(R.string.history_event_scan)
        HistoryEvent.Kind.ANALYSIS -> context.getString(R.string.history_event_analysis, subject)
        HistoryEvent.Kind.DESIGN_OPEN -> context.getString(R.string.history_event_design, subject)
        HistoryEvent.Kind.LOOK_CREATED -> context.getString(R.string.history_event_look, subject)
    }
}
