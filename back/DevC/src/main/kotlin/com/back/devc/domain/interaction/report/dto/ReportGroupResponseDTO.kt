package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import java.time.LocalDateTime

data class ReportGroupResponseDTO(
    val targetType: TargetType,
    val targetId: Long,
    val targetNickname: String?,
    val targetTitle: String?,
    val targetContent: String?,
    val reportCount: Long,
    val reasonTypes: List<String>,
    val status: ReportStatus,
    val latestCreatedAt: LocalDateTime
)
