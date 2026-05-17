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

    val createdAt: LocalDateTime?,

    val processedAt: LocalDateTime?
) {
    companion object {
        fun of(
            report: Report,
            targetNickname: String?,
            targetTitle: String?,
            targetContent: String?
        ): ReportResponseDTO {
            val reporter = call<Any>(report, "getReporter")

            return ReportResponseDTO(
                reportId = call(report, "getReportId"),
                reporterEmail = call(reporter, "getEmail"),
                reporterNickname = call(reporter, "getNickname"),
                targetType = call(report, "getTargetType"),
                targetId = call(report, "getTargetId"),
                targetNickname = targetNickname,
                targetTitle = targetTitle,
                targetContent = targetContent,
                reasonType = call(report, "getReasonType"),
                reasonDetail = call(report, "getReasonDetail"),
                status = call(report, "getStatus"),
                createdAt = call(report, "getCreatedAt"),
                processedAt = call(report, "getProcessedAt")
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> call(target: Any, methodName: String): T =
            target.javaClass.getMethod(methodName).invoke(target) as T
    }
}
