package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "신고 단건 응답")
data class ReportResponseDTO(
    @field:Schema(description = "신고 ID", example = "1")
    val reportId: Long,

    @field:Schema(description = "신고자 이메일", example = "reporter@example.com")
    val reporterEmail: String,

    @field:Schema(description = "신고자 닉네임", example = "reporter")
    val reporterNickname: String,

    @field:Schema(description = "신고 대상 유형", example = "POST")
    val targetType: TargetType,

    @field:Schema(description = "신고 대상 ID", example = "1")
    val targetId: Long,

    @field:Schema(description = "신고 대상 작성자 닉네임", example = "target_user")
    val targetNickname: String?,

    @field:Schema(description = "신고 대상 게시글 제목. 댓글 신고인 경우 null입니다.", example = "신고된 게시글")
    val targetTitle: String?,

    @field:Schema(description = "신고 대상 내용", example = "신고된 콘텐츠 내용")
    val targetContent: String?,

    @field:Schema(description = "신고 사유 유형", example = "SPAM")
    val reasonType: String,

    @field:Schema(description = "신고 상세 사유", example = "광고성 내용이 반복됩니다.")
    val reasonDetail: String?,

    @field:Schema(description = "신고 처리 상태", example = "PENDING")
    val status: ReportStatus,

    @field:Schema(description = "신고 생성 시각", example = "2026-05-20T10:15:30")
    val createdAt: LocalDateTime,

    @field:Schema(description = "신고 처리 시각", example = "2026-05-20T11:00:00")
    val processedAt: LocalDateTime?
) {
    companion object {
        fun of(
            report: Report,
            targetNickname: String?,
            targetTitle: String?,
            targetContent: String?
        ): ReportResponseDTO {
            val reporter = report.reporter

            return ReportResponseDTO(
                reportId = report.reportId
                    ?: throw IllegalStateException("Persisted report id is required for response mapping"),
                reporterEmail = reporter.email,
                reporterNickname = reporter.nickname,
                targetType = report.targetType,
                targetId = report.targetId,
                targetNickname = targetNickname,
                targetTitle = targetTitle,
                targetContent = targetContent,
                reasonType = report.reasonType,
                reasonDetail = report.reasonDetail,
                status = report.status,
                createdAt = report.createdAt
                    ?: throw IllegalStateException("Report creation time is required for response mapping"),
                processedAt = report.processedAt
            )
        }
    }
}
