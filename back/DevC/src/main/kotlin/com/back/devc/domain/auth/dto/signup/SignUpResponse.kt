package com.back.devc.domain.auth.dto.signup

import com.back.devc.domain.member.member.entity.MemberRole
import com.back.devc.domain.member.member.entity.MemberStatus

data class SignUpResponse(
    val userId: Long,
    val email: String,
    val nickname: String,
    val role: MemberRole,
    val status: MemberStatus
)
