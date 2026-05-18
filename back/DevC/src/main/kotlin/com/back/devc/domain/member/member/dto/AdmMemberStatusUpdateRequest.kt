package com.back.devc.domain.member.member.dto

import com.back.devc.domain.member.member.entity.MemberStatus
import jakarta.validation.constraints.NotNull

data class AdmMemberStatusUpdateRequest(
    @field:NotNull(message = "변경할 상태값은 필수입니다.")
    val status: MemberStatus?,
    val days: Int?,
)

