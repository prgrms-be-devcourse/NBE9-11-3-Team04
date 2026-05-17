package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import java.time.LocalDateTime

data class ReportGroupResponseDTO(
    @JvmField
    val targetType: TargetType,

    @JvmField
    val targetId: Long,

    @JvmField
    val targetNickname: String?,

    @JvmField
    val targetTitle: String?,

    @JvmField
    val targetContent: String?,

    @JvmField
    val reportCount: Long,

    @JvmField
    val reasonTypes: List<String>,

    @JvmField
    val status: ReportStatus,

    @JvmField
    val latestCreatedAt: LocalDateTime
)
