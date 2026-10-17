package com.fabricfit.app.data.repository

import com.fabricfit.app.data.local.db.ReportDao
import com.fabricfit.app.data.mapper.toDomain
import com.fabricfit.app.data.mapper.toEntity
import com.fabricfit.app.domain.model.Report
import com.fabricfit.app.domain.model.ReportStatus
import com.fabricfit.app.domain.model.ReportType
import com.fabricfit.app.domain.model.ReportedContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for managing content reports and moderation.
 */
class ReportRepository(
    private val reportDao: ReportDao,
    private val currentUserId: String = "current_user", // In production, get from auth
) {

    /**
     * Submit a new report for content moderation.
     */
    suspend fun submitReport(
        reportedUserId: String,
        contentType: ReportedContentType,
        contentId: String,
        reportType: ReportType,
        description: String = "",
    ): Report {
        val report = Report(
            id = UUID.randomUUID().toString(),
            reporterId = currentUserId,
            reportedUserId = reportedUserId,
            contentType = contentType,
            contentId = contentId,
            reportType = reportType,
            description = description,
        )

        reportDao.insert(report.toEntity())
        return report
    }

    /**
     * Get all reports (for admin/moderator view).
     */
    fun observeAllReports(): Flow<List<Report>> =
        reportDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Get reports submitted by current user.
     */
    fun observeMyReports(): Flow<List<Report>> =
        reportDao.observeByReporter(currentUserId).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Get reports for a specific user (for admin view).
     */
    fun observeReportsForUser(userId: String): Flow<List<Report>> =
        reportDao.observeByReportedUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Get all reports for a specific content item.
     */
    suspend fun getReportsForContent(
        contentId: String,
        contentType: ReportedContentType,
    ): List<Report> =
        reportDao.getByContent(contentId, contentType.name).map { it.toDomain() }

    /**
     * Check if current user has already reported this content.
     */
    suspend fun hasUserReportedContent(
        contentId: String,
        contentType: ReportedContentType,
    ): Boolean {
        val reports = reportDao.getByContent(contentId, contentType.name)
        return reports.any { it.reporterId == currentUserId }
    }

    /**
     * Get count of pending reports for content.
     */
    suspend fun getPendingReportCount(
        contentId: String,
        contentType: ReportedContentType,
    ): Int =
        reportDao.countPendingForContent(contentId, contentType.name)

    /**
     * Update report status (for admin/moderator actions).
     */
    suspend fun updateReportStatus(
        reportId: String,
        status: ReportStatus,
        resolution: String? = null,
    ) {
        val report = reportDao.getById(reportId) ?: return
        reportDao.update(
            report.copy(
                status = status.name,
                resolvedAt = if (status != ReportStatus.PENDING) System.currentTimeMillis() else null,
                resolution = resolution,
            )
        )
    }

    /**
     * Delete a report (for admin actions).
     */
    suspend fun deleteReport(reportId: String) {
        reportDao.delete(reportId)
    }

    /**
     * Get report by ID.
     */
    suspend fun getReport(reportId: String): Report? =
        reportDao.getById(reportId)?.toDomain()
}
