package com.back.devc.domain.interaction.report.dto

import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "신고 그룹 응답")
data class ReportGroupResponseDTO(
    @field:Schema(description = "신고 대상 유형", example = "POST")
    val targetType: TargetType,

    @field:Schema(description = "신고 대상 ID", example = "1")
    val targetId: Long,

    @field:Schema(description = "신고 대상 작성자 닉네임", example = "dev_user")
    val targetNickname: String?,

    @field:Schema(description = "신고 대상 게시글 제목. 댓글 신고인 경우 null입니다.", example = "질문 게시글 제목")
    val targetTitle: String?,

    @field:Schema(description = "신고 대상 내용", example = "신고된 게시글 또는 댓글 내용")
    val targetContent: String?,

    @field:Schema(description = "해당 대상에 누적된 신고 수", example = "3")
    val reportCount: Long,

    @field:Schema(description = "신고 사유 유형 목록", example = "[\"SPAM\", \"ABUSE\"]")
    val reasonTypes: List<String>,

    @field:Schema(description = "신고 처리 상태", example = "PENDING")
    val status: ReportStatus?,

    @field:Schema(description = "마지막 신고 생성 시각", example = "2026-05-20T10:15:30")
    val latestCreatedAt: LocalDateTime
)
