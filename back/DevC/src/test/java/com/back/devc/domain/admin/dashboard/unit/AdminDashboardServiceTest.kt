package com.back.devc.domain.admin.dashboard.unit

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto
import com.back.devc.domain.admin.dashboard.repository.AdminDashboardRepository
import com.back.devc.domain.admin.dashboard.service.AdminDashboardService
import com.back.devc.domain.interaction.report.entity.ReportStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@DisplayName("AdminDashboardService")
class AdminDashboardServiceTest {

    @Test
    @DisplayName("getDashboardData uses injected clock date for daily metrics")
    fun getDashboardDataUsesInjectedClockDate() {
        val repository = RecordingDashboardRepository()
        val clock = Clock.fixed(
            Instant.parse("2024-02-03T12:00:00Z"),
            ZoneId.of("Asia/Seoul"),
        )
        val service = AdminDashboardService(repository, clock)

        val result = service.getDashboardData()

        Assertions.assertThat(repository.requestedDates).containsOnly(LocalDate.of(2024, 2, 3))
        Assertions.assertThat(result.summary.totalUsers).isEqualTo(10L)
        Assertions.assertThat(result.summary.totalPosts).isEqualTo(20L)
        Assertions.assertThat(result.summary.pendingReports).isEqualTo(3L)
        Assertions.assertThat(result.summary.todayVisitors).isZero()
        Assertions.assertThat(result.todayActivity.newUsers).isEqualTo(4L)
        Assertions.assertThat(result.todayActivity.newPosts).isEqualTo(5L)
        Assertions.assertThat(result.todayActivity.newComments).isEqualTo(6L)
        Assertions.assertThat(result.todayReports.post.total).isEqualTo(7L)
        Assertions.assertThat(result.todayReports.post.pending).isEqualTo(8L)
        Assertions.assertThat(result.todayReports.post.resolved).isEqualTo(9L)
        Assertions.assertThat(result.todayReports.post.byReason)
            .containsExactly(DashboardResponseDto.ReportReasonCount("SPAM", 2L))
        Assertions.assertThat(result.todayReports.comment.total).isEqualTo(11L)
        Assertions.assertThat(result.todayReports.comment.pending).isEqualTo(12L)
        Assertions.assertThat(result.todayReports.comment.resolved).isEqualTo(13L)
        Assertions.assertThat(result.todayReports.comment.byReason)
            .containsExactly(DashboardResponseDto.ReportReasonCount("ABUSE", 1L))
    }

    private class RecordingDashboardRepository : AdminDashboardRepository {
        val requestedDates = mutableListOf<LocalDate>()

        override fun countAllUsers(): Long = 10L

        override fun countAllPosts(): Long = 20L

        override fun countPendingReports(): Long = 3L

        override fun countTodayUsers(today: LocalDate): Long {
            requestedDates += today
            return 4L
        }

        override fun countTodayPosts(today: LocalDate): Long {
            requestedDates += today
            return 5L
        }

        override fun countTodayComments(today: LocalDate): Long {
            requestedDates += today
            return 6L
        }

        override fun countTodayPostReports(today: LocalDate): Long {
            requestedDates += today
            return 7L
        }

        override fun countTodayPostReportsByStatus(
            today: LocalDate,
            status: ReportStatus,
        ): Long {
            requestedDates += today
            return when (status) {
                ReportStatus.PENDING -> 8L
                ReportStatus.RESOLVED -> 9L
                ReportStatus.REJECTED -> 0L
            }
        }

        override fun countTodayPostReportsByReason(today: LocalDate): List<DashboardResponseDto.ReportReasonCount> {
            requestedDates += today
            return listOf(DashboardResponseDto.ReportReasonCount("SPAM", 2L))
        }

        override fun countTodayCommentReports(today: LocalDate): Long {
            requestedDates += today
            return 11L
        }

        override fun countTodayCommentReportsByStatus(
            today: LocalDate,
            status: ReportStatus,
        ): Long {
            requestedDates += today
            return when (status) {
                ReportStatus.PENDING -> 12L
                ReportStatus.RESOLVED -> 13L
                ReportStatus.REJECTED -> 0L
            }
        }

        override fun countTodayCommentReportsByReason(today: LocalDate): List<DashboardResponseDto.ReportReasonCount> {
            requestedDates += today
            return listOf(DashboardResponseDto.ReportReasonCount("ABUSE", 1L))
        }
    }
}