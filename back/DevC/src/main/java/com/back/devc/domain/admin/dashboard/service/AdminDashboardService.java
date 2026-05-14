package com.back.devc.domain.admin.dashboard.service;

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto;
import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportReasonCount;
import com.back.devc.domain.admin.dashboard.repository.AdminDashboardRepository;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminDashboardRepository adminDashboardRepository;

    public DashboardResponseDto getDashboardData() {

        LocalDate today = LocalDate.now();

        // =========================
        // 1. 전체 통계
        // =========================
        Long totalUsers = adminDashboardRepository.countAllUsers();
        Long totalPosts = adminDashboardRepository.countAllPosts();
        Long pendingReports = adminDashboardRepository.countPendingReports();

        // =========================
        // 2. 오늘 활동
        // =========================
        Long newUsers = adminDashboardRepository.countTodayUsers(today);
        Long newPosts = adminDashboardRepository.countTodayPosts(today);
        Long newComments = adminDashboardRepository.countTodayComments(today);

        // =========================
        // 3. 게시글 신고
        // =========================
        Long postReportTotal = adminDashboardRepository.countTodayPostReports(today);
        Long postReportPending = adminDashboardRepository.countTodayPostReportsByStatus(today, ReportStatus.PENDING);
        Long postReportResolved = adminDashboardRepository.countTodayPostReportsByStatus(today, ReportStatus.RESOLVED);
        java.util.List<ReportReasonCount> postReportReasons =
                adminDashboardRepository.countTodayPostReportsByReason(today);

        // =========================
        // 4. 댓글 신고
        // =========================
        Long commentReportTotal = adminDashboardRepository.countTodayCommentReports(today);
        Long commentReportPending = adminDashboardRepository.countTodayCommentReportsByStatus(today, ReportStatus.PENDING);
        Long commentReportResolved = adminDashboardRepository.countTodayCommentReportsByStatus(today, ReportStatus.RESOLVED);
        java.util.List<ReportReasonCount> commentReportReasons =
                adminDashboardRepository.countTodayCommentReportsByReason(today);

        // =========================
        // 5. Response 조립
        // =========================
        return DashboardResponseDto.builder()
                .summary(DashboardResponseDto.SummaryStats.builder()
                        .totalUsers(totalUsers)
                        .totalPosts(totalPosts)
                        .pendingReports(pendingReports)
                        .todayVisitors(0L) // 아직 미구현이면 고정
                        .build())
                .todayActivity(DashboardResponseDto.TodayActivity.builder()
                        .newUsers(newUsers)
                        .newPosts(newPosts)
                        .newComments(newComments)
                        .build())
                .todayReports(DashboardResponseDto.TodayReportStats.builder()
                        .post(DashboardResponseDto.ReportCategory.builder()
                                .total(postReportTotal)
                                .pending(postReportPending)
                                .resolved(postReportResolved)
                                .byReason(postReportReasons)
                                .build())
                        .comment(DashboardResponseDto.ReportCategory.builder()
                                .total(commentReportTotal)
                                .pending(commentReportPending)
                                .resolved(commentReportResolved)
                                .byReason(commentReportReasons)
                                .build())
                        .build())
                .build();
    }
}