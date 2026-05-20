package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.SanctionType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "신고 그룹 승인 요청")
data class ApproveReportGroupRequest(
    @field:Schema(description = "관리자 처리 메모", example = "신고가 타당하여 승인합니다.")
    @field:Size(max = 1000, message = "관리자 메모는 1000자를 초과할 수 없습니다.")
    val adminNote: String?,

    @field:Schema(description = "회원 제재 유형", example = "WARNED")
    val sanctionType: SanctionType?,

    @field:Schema(description = "정지 기간(일). 제재 유형이 SUSPENDED인 경우 필요합니다.", example = "7")
    val suspensionDays: Int?
)
