package com.fabricfit.app.domain.model

/**
 * Report types for flagging inappropriate content.
 */
enum class ReportType(val labelRes: Int) {
    SPAM(com.fabricfit.app.R.string.report_type_spam),
    INAPPROPRIATE(com.fabricfit.app.R.string.report_type_inappropriate),
    HARASSMENT(com.fabricfit.app.R.string.report_type_harassment),
    COPYRIGHT(com.fabricfit.app.R.string.report_type_copyright),
    MISLEADING(com.fabricfit.app.R.string.report_type_misleading),
    OTHER(com.fabricfit.app.R.string.report_type_other),
}

/**
 * Report status for tracking moderation.
 */
enum class ReportStatus {
    PENDING,
    UNDER_REVIEW,
    RESOLVED,
    DISMISSED,
}

/**
 * Content type being reported.
 */
enum class ReportedContentType {
    GALLERY_ITEM,
    COMMENT,
    USER,
}

/**
 * Report submitted by a user for content moderation.
 */
data class Report(
    val id: String,
    val reporterId: String,
    val reportedUserId: String,
    val contentType: ReportedContentType,
    val contentId: String,
    val reportType: ReportType,
    val description: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolution: String? = null,
)
