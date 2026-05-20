package com.back.devc.domain.admin.dashboard.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 대시보드 응답")
data class DashboardResponseDto(
    @field:Schema(description = "전체 요약 통계")
    val summary: SummaryStats,

    @field:Schema(description = "오늘 접수된 신고 통계")
    val todayReports: TodayReportStats,

    @field:Schema(description = "오늘 발생한 활동 통계")
    val todayActivity: TodayActivity,
) {
    @Schema(description = "관리자 대시보드 요약 통계")
    data class SummaryStats(
        @field:Schema(description = "전체 회원 수", example = "120")
        val totalUsers: Long,

        @field:Schema(description = "전체 게시글 수", example = "350")
        val totalPosts: Long,

        @field:Schema(description = "처리 대기 중인 신고 수", example = "12")
        val pendingReports: Long,

        @field:Schema(description = "오늘 방문자 수", example = "80")
        val todayVisitors: Long,
    )

    @Schema(description = "오늘 신고 통계")
    data class TodayReportStats(
        @field:Schema(description = "게시글 신고 통계")
        val post: ReportCategory,

        @field:Schema(description = "댓글 신고 통계")
        val comment: ReportCategory,
    )

    @Schema(description = "신고 대상 유형별 통계")
    data class ReportCategory(
        @field:Schema(description = "전체 신고 수", example = "10")
        val total: Long,

        @field:Schema(description = "대기 중인 신고 수", example = "6")
        val pending: Long,

        @field:Schema(description = "처리 완료된 신고 수", example = "4")
        val resolved: Long,

        @field:Schema(description = "신고 사유별 집계")
        val byReason: List<ReportReasonCount>,
    )

    @Schema(description = "신고 사유별 건수")
    data class ReportReasonCount(
        @field:Schema(description = "신고 사유", example = "SPAM")
        val reason: String,

        @field:Schema(description = "신고 건수", example = "5")
        val count: Long,
    )

    @Schema(description = "오늘 활동 통계")
    data class TodayActivity(
        @field:Schema(description = "오늘 생성된 게시글 수", example = "8")
        val newPosts: Long,

        @field:Schema(description = "오늘 생성된 댓글 수", example = "24")
        val newComments: Long,

        @field:Schema(description = "오늘 가입한 회원 수", example = "3")
        val newUsers: Long,
    )
}
