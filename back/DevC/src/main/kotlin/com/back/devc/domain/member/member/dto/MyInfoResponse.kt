package com.back.devc.domain.member.member.dto

import com.back.devc.domain.member.member.entity.MemberRole
import com.back.devc.domain.member.member.entity.MemberStatus
import java.time.LocalDateTime

data class MyInfoResponse(
    val userId: Long,
    val email: String,
    val nickname: String,
    val role: MemberRole,
    val status: MemberStatus,
    val createdAt: LocalDateTime
)
