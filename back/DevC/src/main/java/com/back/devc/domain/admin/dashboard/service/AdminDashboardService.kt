package com.back.devc.domain.admin.dashboard.service

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto
import com.back.devc.domain.admin.dashboard.repository.AdminDashboardRepository
import com.back.devc.domain.interaction.report.entity.ReportStatus
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@RequiredArgsConstructor
class AdminDashboardService {
    private val adminDashboardRepository: AdminDashboardRepository? = null

    val dashboardData: DashboardResponseDto
        get() {
            val today = LocalDate.now()

            // =========================
            // 1. 전체 통계
            // =========================
            val totalUsers = adminDashboardRepository!!.countAllUsers()
            val totalPosts = adminDashboardRepository.countAllPosts()
            val pendingReports = adminDashboardRepository.countPendingReports()

            // =========================
            // 2. 오늘 활동
            // =========================
            val newUsers = adminDashboardRepository.countTodayUsers(today)
            val newPosts = adminDashboardRepository.countTodayPosts(today)
            val newComments = adminDashboardRepository.countTodayComments(today)

            // =========================
            // 3. 게시글 신고
            // =========================
            val postReportTotal = adminDashboardRepository.countTodayPostReports(today)
            val postReportPending = adminDashboardRepository.countTodayPostReportsByStatus(
                today,
                ReportStatus.PENDING
            )
            val postReportResolved = adminDashboardRepository.countTodayPostReportsByStatus(
                today,
                ReportStatus.RESOLVED
            )
            val postReportReasons =
                adminDashboardRepository.countTodayPostReportsByReason(today)

            // =========================
            // 4. 댓글 신고
            // =========================
            val commentReportTotal = adminDashboardRepository.countTodayCommentReports(today)
            val commentReportPending = adminDashboardRepository.countTodayCommentReportsByStatus(
                today,
                ReportStatus.PENDING
            )
            val commentReportResolved = adminDashboardRepository.countTodayCommentReportsByStatus(
                today,
                ReportStatus.RESOLVED
            )
            val commentReportReasons =
                adminDashboardRepository.countTodayCommentReportsByReason(today)

            // =========================
            // 5. Response 조립
            // =========================
            return DashboardResponseDto.builder()
                .summary(
                    DashboardResponseDto.SummaryStats.builder()
                        .totalUsers(totalUsers)
                        .totalPosts(totalPosts)
                        .pendingReports(pendingReports)
                        .todayVisitors(0L) // 아직 미구현이면 고정
                        .build()
                )
                .todayActivity(
                    DashboardResponseDto.TodayActivity.builder()
                        .newUsers(newUsers)
                        .newPosts(newPosts)
                        .newComments(newComments)
                        .build()
                )
                .todayReports(
                    DashboardResponseDto.TodayReportStats.builder()
                        .post(
                            DashboardResponseDto.ReportCategory.builder()
                                .total(postReportTotal)
                                .pending(postReportPending)
                                .resolved(postReportResolved)
                                .byReason(postReportReasons)
                                .build()
                        )
                        .comment(
                            DashboardResponseDto.ReportCategory.builder()
                                .total(commentReportTotal)
                                .pending(commentReportPending)
                                .resolved(commentReportResolved)
                                .byReason(commentReportReasons)
                                .build()
                        )
                        .build()
                )
                .build()
        }
}