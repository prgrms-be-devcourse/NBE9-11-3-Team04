package com.back.devc.domain.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class DashboardResponseDto {

    private SummaryStats summary;
    private TodayReportStats todayReports;
    private TodayActivity todayActivity;

    @Data
    @Builder
    @AllArgsConstructor
    public static class SummaryStats {
        private Long totalUsers;
        private Long totalPosts;
        private Long pendingReports;
        private Long todayVisitors;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class TodayReportStats {
        private ReportCategory post;
        private ReportCategory comment;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ReportCategory {
        private Long total;
        private Long pending;
        private Long resolved;
        private List<ReportReasonCount> byReason;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ReportReasonCount {
        private String reason;
        private Long count;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class TodayActivity {
        private Long newPosts;
        private Long newComments;
        private Long newUsers;
    }
}