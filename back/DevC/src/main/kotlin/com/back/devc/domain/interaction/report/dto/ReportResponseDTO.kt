package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import java.time.LocalDateTime

data class ReportResponseDTO(
    val reportId: Long,
    val reporterEmail: String,
    val reporterNickname: String,
    val targetType: TargetType,
    val targetId: Long,
    val targetNickname: String?,
    val targetTitle: String?,
    val targetContent: String?,
    val reasonType: String,
    val reasonDetail: String?,
    val status: ReportStatus,
    val createdAt: LocalDateTime,
    val processedAt: LocalDateTime?
) {
    companion object {
        fun of(
            report: Report,
            targetNickname: String?,
            targetTitle: String?,
            targetContent: String?
        ): ReportResponseDTO = ReportResponseDTO(
            reportId = report.reportId,
            reporterEmail = report.reporter.email,
            reporterNickname = report.reporter.nickname,
            targetType = report.targetType,
            targetId = report.targetId,
            targetNickname = targetNickname,
            targetTitle = targetTitle,
            targetContent = targetContent,
            reasonType = report.reasonType,
            reasonDetail = report.reasonDetail,
            status = report.status,
            createdAt = report.createdAt,
            processedAt = report.processedAt
        )
    }
}