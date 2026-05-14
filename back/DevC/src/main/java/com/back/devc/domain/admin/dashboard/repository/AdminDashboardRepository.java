package com.back.devc.domain.admin.dashboard.repository;

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportReasonCount;
import com.back.devc.domain.interaction.report.entity.ReportStatus;

import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardRepository {

    Long countAllUsers();
    Long countAllPosts();

    Long countPendingReports();

    Long countTodayUsers(LocalDate today);
    Long countTodayPosts(LocalDate today);
    Long countTodayComments(LocalDate today);

    Long countTodayPostReports(LocalDate today);
    Long countTodayPostReportsByStatus(LocalDate today, ReportStatus status);
    List<ReportReasonCount> countTodayPostReportsByReason(LocalDate today);

    Long countTodayCommentReports(LocalDate today);
    Long countTodayCommentReportsByStatus(LocalDate today, ReportStatus status);
    List<ReportReasonCount> countTodayCommentReportsByReason(LocalDate today);
}