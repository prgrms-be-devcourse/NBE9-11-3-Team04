package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "레거시 신고 그룹 처리 요청")
data class AdminReportRequestDTO(
    @field:Schema(description = "레거시 API에서 targetId로 사용하는 신고 ID 필드", example = "1")
    @field:NotNull(message = "신고 ID는 필수입니다.")
    val reportId: Long,

    @field:Schema(description = "신고 대상 유형", example = "POST")
    @field:NotNull(message = "신고 유형은 필수입니다.")
    val targetType: TargetType,

    @field:Schema(description = "관리자 처리 메모", example = "신고 내용을 확인했습니다.")
    @field:Size(max = 1000, message = "관리자 메모는 1000자를 초과할 수 없습니다.")
    val adminNote: String?,

    @field:Schema(description = "회원 제재 유형", example = "WARNED")
    val sanctionType: SanctionType?,

    @field:Schema(description = "정지 기간(일). 제재 유형이 SUSPENDED인 경우 필요합니다.", example = "7")
    val suspensionDays: Int?
)
