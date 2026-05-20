package com.back.devc.domain.interaction.report.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "신고 그룹 반려 요청")
data class RejectReportGroupRequest(
    @field:Schema(description = "관리자 반려 메모", example = "신고 사유가 충분하지 않아 반려합니다.")
    @field:Size(max = 1000, message = "관리자 메모는 1000자를 초과할 수 없습니다.")
    val adminNote: String?
)
