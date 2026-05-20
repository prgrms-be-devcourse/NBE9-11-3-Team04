package com.back.devc.domain.member.member.dto

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "관리자 회원 목록 응답")
data class AdmMemberListResponse(
    @field:Schema(description = "회원 ID", example = "1")
    val userId: Long,

    @field:Schema(description = "회원 이메일", example = "member@example.com")
    val email: String,

    @field:Schema(description = "회원 닉네임", example = "dev_member")
    val nickname: String,

    @field:Schema(description = "작성한 게시글 수", example = "12")
    val postCount: Long,

    @field:Schema(description = "작성한 댓글 수", example = "34")
    val commentCount: Long,

    @field:Schema(description = "회원 상태", example = "ACTIVE")
    val status: MemberStatus,

    @field:Schema(description = "가입 시각", example = "2026-05-20T10:15:30")
    val createdAt: LocalDateTime,

    @field:Schema(description = "정지 종료 시각. 정지 상태가 아니면 null입니다.", example = "2026-05-27T10:15:30")
    val suspendedUntil: LocalDateTime?,
) {
    companion object {
        @JvmStatic
        fun of(member: Member, postCount: Long, commentCount: Long): AdmMemberListResponse {
            return AdmMemberListResponse(
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

