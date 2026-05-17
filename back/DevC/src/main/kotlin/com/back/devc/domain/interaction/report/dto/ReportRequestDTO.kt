package com.back.devc.domain.interaction.report.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class ReportRequestDTO(
    @field:NotNull(message = "신고 대상 ID는 필수입니다.")
    val targetId: Long,

    @field:NotBlank(message = "신고 사유 유형은 필수입니다.")
    val reasonType: String,

    @field:Size(max = 1000, message = "신고 상세 사유는 1000자를 초과할 수 없습니다.")
    val reasonDetail: String?
)
