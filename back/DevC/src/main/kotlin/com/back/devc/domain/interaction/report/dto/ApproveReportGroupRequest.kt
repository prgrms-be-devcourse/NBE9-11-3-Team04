package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.SanctionType
import jakarta.validation.constraints.Size

data class ApproveReportGroupRequest(
    @field:Size(max = 1000, message = "관리자 메모는 1000자를 초과할 수 없습니다.")
    val adminNote: String?,
    val sanctionType: SanctionType?,
    val suspensionDays: Int?
)
