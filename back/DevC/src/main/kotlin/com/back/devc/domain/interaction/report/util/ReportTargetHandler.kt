package com.back.devc.domain.interaction.report.util

import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.member.service.MemberSanctionService
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.exception.errorCode.ReportErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReportTargetHandler(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val memberRepository: MemberRepository,
    private val notificationService: NotificationService,
    private val memberSanctionService: MemberSanctionService
) {

    companion object {

        private val log = LoggerFactory.getLogger(
            ReportTargetHandler::class.java
        )
    }

    fun handleApproved(
        targetType: TargetType,
        targetId: Long,
        admin: Member,
        sanctionType: SanctionType?,
        suspensionDays: Int?
    ) {

        log.info(
            "신고 승인 대상 처리 시작 - targetType={}, targetId={}, adminId={}, sanctionType={}, suspensionDays={}",
            targetType,
            targetId,
            admin.userId,
            sanctionType,
            suspensionDays
        )

        notify(
            targetType = targetType,
            targetId = targetId,
            admin = admin
        )

        log.info(
            "신고 승인 알림 처리 완료 - targetType={}, targetId={}, adminId={}",
            targetType,
            targetId,
            admin.userId
        )

        deleteTarget(
            targetType = targetType,
            targetId = targetId
        )

        log.info(
            "신고 승인 대상 삭제 처리 완료 - targetType={}, targetId={}",
            targetType,
            targetId
        )

        sanctionType?.let {

            applySanction(
                targetType = targetType,
                targetId = targetId,
                sanctionType = it,
                suspensionDays = suspensionDays
            )

            log.info(
                "신고 승인 제재 처리 완료 - targetType={}, targetId={}, sanctionType={}, suspensionDays={}",
                targetType,
                targetId,
                sanctionType,
                suspensionDays
            )
        }

        log.info(
            "신고 승인 대상 처리 완료 - targetType={}, targetId={}, adminId={}",
            targetType,
            targetId,
            admin.userId
        )
    }

    /* =========================
       NOTIFY
    ========================= */

    private fun notify(
        targetType: TargetType,
        targetId: Long,
        admin: Member
    ) {

        log.info(
            "신고 처리 알림 생성 요청 - targetType={}, targetId={}, adminId={}",
            targetType,
            targetId,
            admin.userId
        )

        when (targetType) {

            TargetType.POST -> {

                notificationService.createPostReportNotification(
                    targetId,
                    admin.requiredUserId
                )

                log.info(
                    "게시글 신고 처리 알림 생성 요청 완료 - postId={}, adminId={}",
                    targetId,
                    admin.userId
                )
            }

            TargetType.COMMENT -> {

                notificationService.createCommentReportNotification(
                    targetId,
                    admin.requiredUserId
                )

                log.info(
                    "댓글 신고 처리 알림 생성 요청 완료 - commentId={}, adminId={}",
                    targetId,
                    admin.userId
                )
            }
        }
    }

    /* =========================
       DELETE
    ========================= */

    private fun deleteTarget(
        targetType: TargetType,
        targetId: Long
    ) {

        log.info(
            "신고 대상 삭제 처리 시작 - targetType={}, targetId={}",
            targetType,
            targetId
        )

        when (targetType) {

            TargetType.POST -> {

                val post = postRepository.findById(targetId)
                    .getOrNull()

                if (post == null) {

                    log.warn(
                        "신고 대상 게시글 삭제 처리 실패 - 게시글 없음, postId={}",
                        targetId
                    )

                    return
                }

                if (!post.isDeleted) {

                    post.delete()

                    log.info(
                        "신고 대상 게시글 삭제 처리 완료 - postId={}",
                        targetId
                    )
                } else {

                    log.info(
                        "신고 대상 게시글 삭제 처리 생략 - 이미 삭제됨, postId={}",
                        targetId
                    )
                }
            }

            TargetType.COMMENT -> {

                val comment = commentRepository.findById(targetId)
                    .getOrNull()

                if (comment == null) {

                    log.warn(
                        "신고 대상 댓글 삭제 처리 실패 - 댓글 없음, commentId={}",
                        targetId
                    )

                    return
                }

                if (!comment.isDeleted) {

                    comment.softDelete()

                    log.info(
                        "신고 대상 댓글 삭제 처리 완료 - commentId={}",
                        targetId
                    )
                } else {

                    log.info(
                        "신고 대상 댓글 삭제 처리 생략 - 이미 삭제됨, commentId={}",
                        targetId
                    )
                }
            }
        }
    }

    /* =========================
       SANCTION
    ========================= */

    private fun applySanction(
        targetType: TargetType,
        targetId: Long,
        sanctionType: SanctionType,
        suspensionDays: Int?
    ) {

        log.info(
            "신고 대상 작성자 제재 처리 시작 - targetType={}, targetId={}, sanctionType={}, suspensionDays={}",
            targetType,
            targetId,
            sanctionType,
            suspensionDays
        )

        val member = findTargetMember(
            targetType = targetType,
            targetId = targetId
        )

        val status = when (sanctionType) {
            SanctionType.WARNED -> MemberStatus.WARNED
            SanctionType.SUSPENDED -> MemberStatus.SUSPENDED
            SanctionType.BLACKLISTED -> MemberStatus.BLACKLISTED
        }

        log.info(
            "신고 대상 작성자 제재 상태 결정 - targetType={}, targetId={}, targetUserId={}, sanctionType={}, memberStatus={}, suspensionDays={}",
            targetType,
            targetId,
            member.userId,
            sanctionType,
            status,
            suspensionDays
        )

        memberSanctionService.apply(
            member,
            status,
            suspensionDays
        )

        log.info(
            "신고 대상 작성자 제재 처리 완료 - targetType={}, targetId={}, targetUserId={}, memberStatus={}, suspensionDays={}",
            targetType,
            targetId,
            member.userId,
            status,
            suspensionDays
        )
    }

    private fun findTargetMember(
        targetType: TargetType,
        targetId: Long
    ): Member {

        log.debug(
            "신고 대상 작성자 조회 시작 - targetType={}, targetId={}",
            targetType,
            targetId
        )

        return when (targetType) {

            TargetType.POST -> {

                val post = postRepository.findById(targetId)
                    .getOrThrow(ReportErrorCode.REPORT_404_TARGET_USER)

                post.member
            }

            TargetType.COMMENT -> {

                val comment = commentRepository.findById(targetId)
                    .getOrThrow(ReportErrorCode.REPORT_404_TARGET_USER)

                memberRepository.findById(
                    comment.writerId
                ).getOrThrow(ReportErrorCode.REPORT_404_TARGET_USER)
            }
        }
    }

    @Transactional(readOnly = true)
    fun toDtoWithTargetInfo(
        report: Report
    ): ReportResponseDTO {

        val info = getTargetInfo(
            report.targetType,
            report.targetId
        )

        return ReportResponseDTO.of(
            report,
            info.nickname,
            info.title,
            info.content
        )
    }

    fun getTargetInfo(
        targetType: TargetType,
        targetId: Long
    ): TargetInfo {

        log.debug(
            "신고 대상 정보 조회 시작 - targetType={}, targetId={}",
            targetType,
            targetId
        )

        return when (targetType) {

            TargetType.POST -> {

                val post = postRepository.findById(targetId)
                    .getOrNull()

                if (post == null) {

                    log.warn(
                        "신고 대상 게시글 정보 조회 실패 - postId={}",
                        targetId
                    )

                    return TargetInfo(
                        nickname = null,
                        title = null,
                        content = null
                    )
                }

                TargetInfo(
                    nickname = post.member.nickname,
                    title = post.title,
                    content = post.content
                )
            }

            TargetType.COMMENT -> {

                val comment = commentRepository.findById(targetId)
                    .getOrNull()

                if (comment == null) {

                    log.warn(
                        "신고 대상 댓글 정보 조회 실패 - commentId={}",
                        targetId
                    )

                    return TargetInfo(
                        nickname = null,
                        title = null,
                        content = null
                    )
                }

                val nickname = memberRepository.findById(
                    comment.writerId
                )
                    .map(Member::nickname)
                    .getOrNull()

                TargetInfo(
                    nickname = nickname,
                    title = null,
                    content = comment.content
                )
            }
        }
    }

    fun handleRejected(
        targetType: TargetType,
        targetId: Long,
        admin: Member
    ) {

        log.info(
            "신고 반려 대상 처리 시작 - targetType={}, targetId={}, adminId={}",
            targetType,
            targetId,
            admin.userId
        )

        notify(
            targetType = targetType,
            targetId = targetId,
            admin = admin
        )

        log.info(
            "신고 반려 대상 처리 완료 - targetType={}, targetId={}, adminId={}",
            targetType,
            targetId,
            admin.userId
        )
    }

    @Transactional(readOnly = true)
    fun exists(
        targetType: TargetType,
        targetId: Long
    ): Boolean {

        val exists = when (targetType) {

            TargetType.POST -> {
                postRepository.existsById(targetId)
            }

            TargetType.COMMENT -> {
                commentRepository.existsById(targetId)
            }
        }

        if (!exists) {

            log.warn(
                "신고 대상 존재 여부 확인 실패 - targetType={}, targetId={}",
                targetType,
                targetId
            )
        } else {

            log.debug(
                "신고 대상 존재 여부 확인 완료 - targetType={}, targetId={}",
                targetType,
                targetId
            )
        }

        return exists
    }

    data class TargetInfo(
        val nickname: String?,
        val title: String?,
        val content: String?
    )

    private val Member.requiredUserId: Long
        get() = userId
            ?: throw ApiException(MemberErrorCode.MEMBER_NOT_FOUND)

    private val Comment.writerId: Long
        get() = getUserId()
}
