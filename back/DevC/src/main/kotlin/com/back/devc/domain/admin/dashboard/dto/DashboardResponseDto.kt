package com.back.devc.domain.admin.dashboard.dto

data class DashboardResponseDto(
    val summary: SummaryStats,
    val todayReports: TodayReportStats,
    val todayActivity: TodayActivity,
) {
    data class SummaryStats(
        val totalUsers: Long,
        val totalPosts: Long,
        val pendingReports: Long,
        val todayVisitors: Long,
    )

    data class TodayReportStats(
        val post: ReportCategory,
        val comment: ReportCategory,
    )

    data class ReportCategory(
        val total: Long,
        val pending: Long,
        val resolved: Long,
        val byReason: List<ReportReasonCount>,
    )

    data class ReportReasonCount(
        val reason: String,
        val count: Long,
    )

    data class TodayActivity(
        val newPosts: Long,
        val newComments: Long,
        val newUsers: Long,
    )
}
