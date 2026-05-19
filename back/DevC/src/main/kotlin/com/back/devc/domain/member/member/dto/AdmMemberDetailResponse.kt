package com.back.devc.domain.member.member.dto

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import java.time.LocalDateTime

data class AdmMemberDetailResponse(
    val userId: Long,
    val email: String,
    val nickname: String,
    val postCount: Long,
    val commentCount: Long,
    val status: MemberStatus,
    val createdAt: LocalDateTime,
    val suspendedUntil: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun of(member: Member, postCount: Long, commentCount: Long): AdmMemberDetailResponse {
            return AdmMemberDetailResponse(
                userId = requireNotNull(member.userId) { "Member userId must not be null" },
                email = member.email,
                nickname = member.nickname,
                postCount = postCount,
                commentCount = commentCount,
                status = member.status,
                createdAt = requireNotNull(member.createdAt) { "Member createdAt must not be null" },
                suspendedUntil = member.suspendedUntil,
            )
        }
    }
}

