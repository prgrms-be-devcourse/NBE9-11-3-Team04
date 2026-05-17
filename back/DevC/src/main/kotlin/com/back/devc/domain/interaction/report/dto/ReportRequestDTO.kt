package com.back.devc.domain.interaction.report.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ReportRequestDTO(
    val targetId: Long,
    val reasonType: String,
    @field:Size(max = 1000, message = "신고 상세 사유는 1000자를 초과할 수 없습니다.")
    val reasonDetail: String?
)