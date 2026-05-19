package com.back.devc.domain.admin.dashboard.dto

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data

@Data
@Builder
@AllArgsConstructor
class DashboardResponseDto {
    private val summary: SummaryStats? = null
    private val todayReports: TodayReportStats? = null
    private val todayActivity: TodayActivity? = null

    @Data
    @Builder
    @AllArgsConstructor
    class SummaryStats {
        private val totalUsers: Long? = null
        private val totalPosts: Long? = null
        private val pendingReports: Long? = null
        private val todayVisitors: Long? = null
    }

    @Data
    @Builder
    @AllArgsConstructor
    class TodayReportStats {
        private val post: ReportCategory? = null
        private val comment: ReportCategory? = null
    }

    @Data
    @Builder
    @AllArgsConstructor
    class ReportCategory {
        private val total: Long? = null
        private val pending: Long? = null
        private val resolved: Long? = null
        private val byReason: MutableList<ReportReasonCount?>? = null
    }

    @Data
    @Builder
    @AllArgsConstructor
    class ReportReasonCount {
        private val reason: String? = null
        private val count: Long? = null
    }

    @Data
    @Builder
    @AllArgsConstructor
    class TodayActivity {
        private val newPosts: Long? = null
        private val newComments: Long? = null
        private val newUsers: Long? = null
    }
}