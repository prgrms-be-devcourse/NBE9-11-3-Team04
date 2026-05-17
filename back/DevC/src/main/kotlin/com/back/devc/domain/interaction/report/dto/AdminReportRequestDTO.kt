package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AdminReportRequestDTO(
    @field:NotNull(message = "신고 ID는 필수입니다.")
    @JvmField
    val reportId: Long,

    @field:NotNull(message = "신고 유형은 필수입니다.")
    @JvmField
    val targetType: TargetType,

    @field:Size(max = 1000, message = "관리자 메모는 1000자를 초과할 수 없습니다.")
    @JvmField
    val adminNote: String?,

    @JvmField
    val sanctionType: SanctionType?,

    @JvmField
    val suspensionDays: Int?
)
