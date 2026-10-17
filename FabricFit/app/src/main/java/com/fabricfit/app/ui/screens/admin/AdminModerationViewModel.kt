package com.fabricfit.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.Report
import com.fabricfit.app.domain.model.ReportStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for admin moderation dashboard.
 */
class AdminModerationViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminModerationState())
    val state: StateFlow<AdminModerationState> = _state.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            container.reports.observeAllReports().collect { reports ->
                _state.update { state ->
                    state.copy(
                        allReports = reports,
                        filteredReports = filterReports(reports, state.filter),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setFilter(filter: ReportFilter) {
        _state.update { state ->
            state.copy(
                filter = filter,
                filteredReports = filterReports(state.allReports, filter),
            )
        }
    }

    fun setSortOrder(sortOrder: ReportSortOrder) {
        _state.update { state ->
            val sorted = when (sortOrder) {
                ReportSortOrder.NEWEST_FIRST -> state.filteredReports.sortedByDescending { it.createdAt }
                ReportSortOrder.OLDEST_FIRST -> state.filteredReports.sortedBy { it.createdAt }
                ReportSortOrder.SEVERITY -> state.filteredReports.sortedByDescending { it.reportType.ordinal }
            }
            state.copy(
                sortOrder = sortOrder,
                filteredReports = sorted,
            )
        }
    }

    fun toggleReportSelection(reportId: String) {
        _state.update { state ->
            val selected = state.selectedReportIds.toMutableSet()
            if (selected.contains(reportId)) {
                selected.remove(reportId)
            } else {
                selected.add(reportId)
            }
            state.copy(selectedReportIds = selected)
        }
    }

    fun selectAllReports() {
        _state.update { state ->
            val allIds = state.filteredReports.map { it.id }.toSet()
            state.copy(selectedReportIds = allIds)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedReportIds = emptySet()) }
    }

    fun dismissReport(reportId: String) {
        viewModelScope.launch {
            container.reports.updateReportStatus(reportId, ReportStatus.DISMISSED)
        }
    }

    fun takeActionOnReport(reportId: String) {
        viewModelScope.launch {
            container.reports.updateReportStatus(reportId, ReportStatus.ACTION_TAKEN)
        }
    }

    fun bulkDismiss() {
        viewModelScope.launch {
            _state.value.selectedReportIds.forEach { reportId ->
                container.reports.updateReportStatus(reportId, ReportStatus.DISMISSED)
            }
            clearSelection()
        }
    }

    fun bulkTakeAction() {
        viewModelScope.launch {
            _state.value.selectedReportIds.forEach { reportId ->
                container.reports.updateReportStatus(reportId, ReportStatus.ACTION_TAKEN)
            }
            clearSelection()
        }
    }

    private fun filterReports(
        reports: List<Report>,
        filter: ReportFilter,
    ): List<Report> {
        return when (filter) {
            ReportFilter.ALL -> reports
            ReportFilter.PENDING -> reports.filter { it.status == ReportStatus.PENDING }
            ReportFilter.REVIEWED -> reports.filter { it.status == ReportStatus.REVIEWED }
            ReportFilter.DISMISSED -> reports.filter { it.status == ReportStatus.DISMISSED }
            ReportFilter.ACTION_TAKEN -> reports.filter { it.status == ReportStatus.ACTION_TAKEN }
        }
    }
}

data class AdminModerationState(
    val allReports: List<Report> = emptyList(),
    val filteredReports: List<Report> = emptyList(),
    val selectedReportIds: Set<String> = emptySet(),
    val filter: ReportFilter = ReportFilter.PENDING,
    val sortOrder: ReportSortOrder = ReportSortOrder.NEWEST_FIRST,
    val isLoading: Boolean = true,
)

enum class ReportFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    REVIEWED("Reviewed"),
    DISMISSED("Dismissed"),
    ACTION_TAKEN("Action Taken"),
}

enum class ReportSortOrder(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    SEVERITY("Severity"),
}
