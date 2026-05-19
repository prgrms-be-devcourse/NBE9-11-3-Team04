package com.back.devc.domain.admin.dashboard.service

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto
import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportCategory
import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportReasonCount
import com.back.devc.domain.admin.dashboard.repository.AdminDashboardRepository
import com.back.devc.domain.interaction.report.entity.ReportStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminDashboardService {
    private val adminDashboardRepository: AdminDashboardRepository
    private val clock: Clock

    @Autowired
    constructor(adminDashboardRepository: AdminDashboardRepository) : this(
        adminDashboardRepository,
        Clock.systemDefaultZone(),
    )

    constructor(
        adminDashboardRepository: AdminDashboardRepository,
        clock: Clock,
    ) {
        this.adminDashboardRepository = adminDashboardRepository
        this.clock = clock
    }

    fun getDashboardData(): DashboardResponseDto {
        val today = LocalDate.now(clock)
        val postReportReasons = adminDashboardRepository.countTodayPostReportsByReason(today)
        val commentReportReasons = adminDashboardRepository.countTodayCommentReportsByReason(today)

        return DashboardResponseDto(
            summary = DashboardResponseDto.SummaryStats(
                totalUsers = adminDashboardRepository.countAllUsers(),
                totalPosts = adminDashboardRepository.countAllPosts(),
                pendingReports = adminDashboardRepository.countPendingReports(),
                todayVisitors = 0L,
            ),
            todayActivity = DashboardResponseDto.TodayActivity(
                newUsers = adminDashboardRepository.countTodayUsers(today),
                newPosts = adminDashboardRepository.countTodayPosts(today),
                newComments = adminDashboardRepository.countTodayComments(today),
            ),
            todayReports = DashboardResponseDto.TodayReportStats(
                post = buildPostReportCategory(today, postReportReasons),
                comment = buildCommentReportCategory(today, commentReportReasons),
            ),
        )
    }

    private fun buildPostReportCategory(
        today: LocalDate,
        byReason: List<ReportReasonCount>,
    ): ReportCategory {
        return ReportCategory(
            total = adminDashboardRepository.countTodayPostReports(today),
            pending = adminDashboardRepository.countTodayPostReportsByStatus(today, ReportStatus.PENDING),
            resolved = adminDashboardRepository.countTodayPostReportsByStatus(today, ReportStatus.RESOLVED),
            byReason = byReason,
        )
    }

    private fun buildCommentReportCategory(
        today: LocalDate,
        byReason: List<ReportReasonCount>,
    ): ReportCategory {
        return ReportCategory(
            total = adminDashboardRepository.countTodayCommentReports(today),
            pending = adminDashboardRepository.countTodayCommentReportsByStatus(today, ReportStatus.PENDING),
            resolved = adminDashboardRepository.countTodayCommentReportsByStatus(today, ReportStatus.RESOLVED),
            byReason = byReason,
        )
    }
}
