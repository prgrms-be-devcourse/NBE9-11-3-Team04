package com.back.devc.domain.auth.dto.login

import com.back.devc.domain.member.member.entity.MemberRole
import com.back.devc.domain.member.member.entity.MemberStatus

data class LoginResponse(
    val userId: Long,
    val email: String,
    val nickname: String,
    val role: MemberRole,
    val status: MemberStatus,
    val accessToken: String
)
