package com.back.devc.domain.admin.dashboard.repository

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportReasonCount
import com.back.devc.domain.interaction.report.entity.ReportStatus
import java.time.LocalDate

interface AdminDashboardRepository {
    fun countAllUsers(): Long

    fun countAllPosts(): Long

    fun countPendingReports(): Long

    fun countTodayUsers(today: LocalDate): Long

    fun countTodayPosts(today: LocalDate): Long

    fun countTodayComments(today: LocalDate): Long

    fun countTodayPostReports(today: LocalDate): Long

    fun countTodayPostReportsByStatus(today: LocalDate, status: ReportStatus): Long

    fun countTodayPostReportsByReason(today: LocalDate): List<ReportReasonCount>

    fun countTodayCommentReports(today: LocalDate): Long

    fun countTodayCommentReportsByStatus(today: LocalDate, status: ReportStatus): Long

    fun countTodayCommentReportsByReason(today: LocalDate): List<ReportReasonCount>
}
