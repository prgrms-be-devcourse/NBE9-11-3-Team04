package com.back.devc.domain.member.member.dto

import com.back.devc.domain.member.member.entity.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "관리자 회원 상태 변경 요청")
data class AdmMemberStatusUpdateRequest(
    @field:Schema(description = "변경할 회원 상태", example = "SUSPENDED")
    @field:NotNull(message = "변경할 상태값은 필수입니다.")
    val status: MemberStatus?,

    @field:Schema(description = "정지 기간(일). 상태가 SUSPENDED인 경우 사용합니다.", example = "7")
    val days: Int?,
)

