package com.back.devc.domain.interaction.report.service

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.exception.errorCode.ReportErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class UserReportService(
    private val reportRepository: ReportRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository
) {

    /* =========================================================
     * Public API
     * ========================================================= */

    fun reportPost(
        reporterId: Long,
        dto: ReportRequestDTO
    ) {
        report(
            reporterId = reporterId,
            targetType = TargetType.POST,
            dto = dto
        )
    }

    fun reportComment(
        reporterId: Long,
        dto: ReportRequestDTO
    ) {
        report(
            reporterId = reporterId,
            targetType = TargetType.COMMENT,
            dto = dto
        )
    }

    /* =========================================================
     * Report
     * ========================================================= */

    private fun report(
        reporterId: Long,
        targetType: TargetType,
        dto: ReportRequestDTO
    ) {

        val reporter = findMemberOrThrow(reporterId)

        validateTarget(
            reporterId = reporterId,
            type = targetType,
            targetId = dto.targetId
        )

        validateDuplicateReport(
            reporter = reporter,
            type = targetType,
            targetId = dto.targetId
        )

        val report = Report(
            reporter = reporter,
            targetType = targetType,
            targetId = dto.targetId,
            reasonType = dto.reasonType,
            reasonDetail = dto.reasonDetail
        )

        reportRepository.save(report)
    }

    /* =========================================================
     * Validation
     * ========================================================= */

    private fun validateTarget(
        reporterId: Long,
        type: TargetType,
        targetId: Long
    ) {

        when (type) {

            TargetType.POST -> {
                validatePostTarget(
                    reporterId = reporterId,
                    targetId = targetId
                )
            }

            TargetType.COMMENT -> {
                validateCommentTarget(
                    reporterId = reporterId,
                    targetId = targetId
                )
            }
        }
    }

    private fun validatePostTarget(
        reporterId: Long,
        targetId: Long
    ) {

        val post = postRepository.findById(targetId)
            .orElseThrow {
                ApiException(
                    ReportErrorCode.REPORT_404_TARGET
                )
            }

        if (
            requireNotNull(post.member.userId) == reporterId
        ) {
            throw ApiException(
                ReportErrorCode.REPORT_400_REPORT_SELF
            )
        }

        if (post.isDeleted) {
            throw ApiException(
                ReportErrorCode.REPORT_410_ALREADY_DELETED
            )
        }
    }

    private fun validateCommentTarget(
        reporterId: Long,
        targetId: Long
    ) {

        val comment = commentRepository.findById(targetId)
            .orElseThrow {
                ApiException(
                    ReportErrorCode.REPORT_404_TARGET
                )
            }

        if (
            requireNotNull(comment.getUserId()) == reporterId
        ) {
            throw ApiException(
                ReportErrorCode.REPORT_400_REPORT_SELF
            )
        }

        if (comment.isDeleted) {
            throw ApiException(
                ReportErrorCode.REPORT_410_ALREADY_DELETED
            )
        }
    }

    private fun validateDuplicateReport(
        reporter: Member,
        type: TargetType,
        targetId: Long
    ) {

        if (
            reportRepository.existsByReporterAndTargetTypeAndTargetId(
                reporter,
                type,
                targetId
            )
        ) {
            throw ApiException(
                ReportErrorCode.REPORT_409_ALREADY_REPORT_USER
            )
        }
    }

    private fun findMemberOrThrow(
        userId: Long
    ): Member {

        return memberRepository.findById(userId)
            .orElseThrow {
                ApiException(
                    MemberErrorCode.MEMBER_NOT_FOUND
                )
            }
    }
}
