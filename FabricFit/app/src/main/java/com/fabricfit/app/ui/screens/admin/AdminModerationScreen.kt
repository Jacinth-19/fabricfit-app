package com.fabricfit.app.ui.screens.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.fabricfit.app.R
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.Report
import com.fabricfit.app.domain.model.ReportStatus
import com.fabricfit.app.ui.navigation.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin moderation dashboard for reviewing reported content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationScreen(
    nav: NavHostController,
    padding: PaddingValues,
) {
    val vm: AdminModerationViewModel = appViewModel { c, ctx ->
        AdminModerationViewModel(AppContainer.get(ctx))
    }
    val state by vm.state.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        // Top app bar
        TopAppBar(
            title = { Text(stringResource(R.string.admin_moderation_title)) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
            },
            actions = {
                // Sort menu
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        ReportSortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = {
                                    vm.setSortOrder(order)
                                    showSortMenu = false
                                },
                            )
                        }
                    }
                }
            },
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportFilter.values().forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { vm.setFilter(filter) },
                    label = { Text(filter.label) },
                    leadingIcon = if (state.filter == filter) {
                        { Icon(Icons.Filled.FilterList, contentDescription = null) }
                    } else null,
                )
            }
        }

        // Bulk actions bar (when items selected)
        if (state.selectedReportIds.isNotEmpty()) {
            BulkActionsBar(
                selectedCount = state.selectedReportIds.size,
                onDismiss = { vm.bulkDismiss() },
                onTakeAction = { vm.bulkTakeAction() },
                onClearSelection = { vm.clearSelection() },
            )
        }

        // Content
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.filteredReports.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.admin_no_reports),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.filteredReports) { report ->
                        ReportCard(
                            report = report,
                            isSelected = state.selectedReportIds.contains(report.id),
                            onToggleSelection = { vm.toggleReportSelection(report.id) },
                            onViewDetails = { selectedReport = report },
                            onDismiss = { vm.dismissReport(report.id) },
                            onTakeAction = { vm.takeActionOnReport(report.id) },
                        )
                    }
                }
            }
        }
    }

    // Report detail dialog
    if (selectedReport != null) {
        ReportDetailDialog(
            report = selectedReport!!,
            onDismiss = { selectedReport = null },
            onDismissReport = {
                vm.dismissReport(selectedReport!!.id)
                selectedReport = null
            },
            onTakeAction = {
                vm.takeActionOnReport(selectedReport!!.id)
                selectedReport = null
            },
        )
    }
}

@Composable
private fun BulkActionsBar(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onTakeAction: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearSelection) {
                    Text(stringResource(R.string.action_clear))
                }
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.admin_dismiss))
                }
                Button(onClick = onTakeAction) {
                    Text(stringResource(R.string.admin_take_action))
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: Report,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit,
    onTakeAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = null,
                    tint = getStatusColor(report.status),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.reportType.name.replace("_", " "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = formatDate(report.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = getStatusColor(report.status).copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = report.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(report.status),
                        fontWeight = FontWeight.Medium,
                    )
                }

                if (report.status == ReportStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onTakeAction) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Take Action",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportDetailDialog(
    report: Report,
    onDismiss: () -> Unit,
    onDismissReport: () -> Unit,
    onTakeAction: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = report.reportType.name.replace("_", " "),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.report_details_reported_user),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = report.reportedUserId)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.report_details_content_type),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = report.contentType.name)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.report_details_description),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = report.description)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.report_details_date),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = formatDate(report.createdAt))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.report_details_status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = report.status.name)
            }
        },
        confirmButton = {
            if (report.status == ReportStatus.PENDING) {
                Button(onClick = onTakeAction) {
                    Text(stringResource(R.string.admin_take_action))
                }
            }
        },
        dismissButton = {
            if (report.status == ReportStatus.PENDING) {
                OutlinedButton(onClick = onDismissReport) {
                    Text(stringResource(R.string.admin_dismiss))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }
        },
    )
}

private fun getStatusColor(status: ReportStatus): Color {
    return when (status) {
        ReportStatus.PENDING -> Color(0xFFFF9800) // Orange
        ReportStatus.UNDER_REVIEW -> Color(0xFF2196F3) // Blue
        ReportStatus.DISMISSED -> Color(0xFF9E9E9E) // Gray
        ReportStatus.RESOLVED -> Color(0xFF4CAF50) // Green
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
