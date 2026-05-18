package com.back.devc.domain.interaction.notification.service

import com.back.devc.domain.interaction.notification.dto.NotificationListResponse
import com.back.devc.domain.interaction.notification.dto.NotificationResponse
import com.back.devc.domain.interaction.notification.entity.Notification
import com.back.devc.domain.interaction.notification.repository.NotificationRepository
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.comment.service.CommentService
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.NotificationErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import kotlin.math.max
import kotlin.math.min

/**
 * 알림 비즈니스 로직 서비스
 *
 * 이 서비스는 "누가(receiver) 어떤 행동(actor)을 알림으로 받아야 하는지"를 결정하고,
 * 그 결과를 Notification 엔티티로 저장하는 역할을 한다.
 *
 * 현재 이 프로젝트에서 다루는 알림 종류
 * - COMMENT : 내 게시글에 다른 사용자가 댓글을 남긴 경우
 * - REPLY   : 내 댓글에 다른 사용자가 답글을 남긴 경우
 * - LIKE    : 내 게시글에 다른 사용자가 좋아요를 누른 경우
 * - BOOKMARK: 내 게시글을 다른 사용자가 북마크한 경우
 * - REPORT  : 관리자 처리 후 내 게시글/댓글이 신고된 사실을 안내하는 경우
 *
 * 구현 시 주의한 점
 * - 자기 자신이 한 행동은 알림을 만들지 않는다.
 * - soft delete 된 부모 댓글에는 답글 알림을 만들지 않는다.
 * - 좋아요 알림은 취소 후 다시 눌렀을 때 중복 생성되지 않도록 한 번만 만든다.
 */
@Service
@Transactional(readOnly = true)
class NotificationService(
    // 알림 저장/조회에 사용하는 JPA Repository.
    private val notificationRepository: NotificationRepository,

    // 답글 알림 생성 시 부모 댓글 상태(존재 여부, 삭제 여부, 작성자)를 확인할 때 사용
    private val commentRepository: CommentRepository,

    // actorUserId로 회원 닉네임을 조회해 알림 메시지/응답에 사용
    private val memberRepository: MemberRepository,

    // 게시글 작성자(userId)를 찾아 "알림 수신자"를 결정할 때 사용
    private val postRepository: PostRepository,
) {

    /**
     * 댓글 저장 트랜잭션이 정상 커밋된 이후 댓글 알림을 생성한다.
     * 알림 생성에 실패해도 이미 커밋된 댓글 작성 결과에는 영향을 주지 않도록 예외를 삼킨다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleCommentCreatedEvent(event: CommentService.CommentCreatedEvent) {
        log.info(
            "댓글 생성 이벤트 수신 - postId={}, actorUserId={}, commentId={}",
            event.postId,
            event.actorUserId,
            event.commentId,
        )
        try {
            createCommentNotification(event.postId, event.actorUserId, event.commentId)
        } catch (e: Exception) {
            log.warn(
                "댓글 알림 생성 실패 - postId={}, actorUserId={}, commentId={}",
                event.postId,
                event.actorUserId,
                event.commentId,
                e,
            )
        }
    }

    /**
     * 대댓글 저장 트랜잭션이 정상 커밋된 이후 대댓글 알림을 생성한다.
     * 알림 생성에 실패해도 이미 커밋된 대댓글 작성 결과에는 영향을 주지 않도록 예외를 삼킨다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleReplyCreatedEvent(event: CommentService.ReplyCreatedEvent) {
        log.info(
            "대댓글 생성 이벤트 수신 - parentCommentId={}, actorUserId={}, replyCommentId={}",
            event.parentCommentId,
            event.actorUserId,
            event.replyCommentId,
        )
        try {
            createReplyNotification(event.parentCommentId, event.actorUserId, event.replyCommentId)
        } catch (e: Exception) {
            log.warn(
                "대댓글 알림 생성 실패 - parentCommentId={}, actorUserId={}, replyCommentId={}",
                event.parentCommentId,
                event.actorUserId,
                event.replyCommentId,
                e,
            )
        }
    }

    /**
     * 게시글 댓글 알림 생성
     *
     * 흐름
     * 1) 게시글 작성자(userId)를 찾음
     * 2) 댓글 작성자가 본인 자신이면 알림을 만들지 않음
     * 3) 게시글 작성자를 receiver, 댓글 작성자를 actor로 하여 COMMENT 알림을 저장
     */
    @Transactional
    fun createCommentNotification(postId: Long, actorUserId: Long, commentId: Long?) {
        log.info(
            "댓글 알림 생성 시작 - postId={}, actorUserId={}, commentId={}",
            postId,
            actorUserId,
            commentId,
        )
        val postOwnerId = findPostOwnerId(postId)

        if (postOwnerId == actorUserId) {
            log.info("댓글 알림 생성 생략 - 본인 게시글 댓글, postId={}, actorUserId={}", postId, actorUserId)
            return
        }

        val actorNickname = findMemberNickname(actorUserId)

        saveNotification(
            receiverUserId = postOwnerId,
            actorUserId = actorUserId,
            postId = postId,
            commentId = commentId,
            type = "COMMENT",
            message = "${actorNickname}님이 게시글에 댓글을 남겼습니다.",
        )

        log.info(
            "댓글 알림 생성 완료 - receiverUserId={}, actorUserId={}, postId={}, commentId={}",
            postOwnerId,
            actorUserId,
            postId,
            commentId,
        )
    }

    /**
     * 대댓글(답글) 알림 생성
     *
     * 흐름
     * 1) 부모 댓글을 조회
     * 2) 부모 댓글이 soft delete 상태면 알림을 만들지 않음
     * 3) 부모 댓글 작성자를 receiver로 잡음
     * 4) 답글 작성자가 본인 자신이면 알림을 만들지 않음
     * 5) 부모 댓글 작성자에게 REPLY 알림을 저장
     */
    @Transactional
    fun createReplyNotification(parentCommentId: Long, actorUserId: Long, replyCommentId: Long?) {
        log.info(
            "대댓글 알림 생성 시작 - parentCommentId={}, actorUserId={}, replyCommentId={}",
            parentCommentId,
            actorUserId,
            replyCommentId,
        )
        val parentComment = findCommentOrThrow(parentCommentId)

        if (parentComment.isDeleted) {
            log.info(
                "대댓글 알림 생성 생략 - 삭제된 부모 댓글, parentCommentId={}, actorUserId={}",
                parentCommentId,
                actorUserId,
            )
            return
        }

        val receiverUserId = parentComment.getUserId()

        if (receiverUserId == actorUserId) {
            log.info(
                "대댓글 알림 생성 생략 - 본인 댓글 답글, parentCommentId={}, actorUserId={}",
                parentCommentId,
                actorUserId,
            )
            return
        }

        val actorNickname = findMemberNickname(actorUserId)
        val postId = requireNotNull(parentComment.postId)

        saveNotification(
            receiverUserId = receiverUserId,
            actorUserId = actorUserId,
            postId = postId,
            commentId = replyCommentId,
            type = "REPLY",
            message = "${actorNickname}님이 회원님의 댓글에 답글을 남겼습니다.",
        )

        log.info(
            "대댓글 알림 생성 완료 - receiverUserId={}, actorUserId={}, postId={}, replyCommentId={}",
            receiverUserId,
            actorUserId,
            postId,
            replyCommentId,
        )
    }

    /**
     * 게시글 좋아요 알림 생성
     *
     * 주의 사항
     * - 자기 자신의 게시글에 좋아요를 누른 경우 알림을 만들지 않음
     * - 같은 사용자가 같은 게시글에 대해 좋아요 취소 후 다시 눌러도
     * LIKE 알림은 한 번만 남기도록 중복 생성 방지 검사를 수행
     */
    @Transactional
    fun createPostLikeNotification(postId: Long, actorUserId: Long) {
        log.info("좋아요 알림 생성 시작 - postId={}, actorUserId={}", postId, actorUserId)
        val postOwnerId = findPostOwnerId(postId)

        if (postOwnerId == actorUserId) {
            log.info("좋아요 알림 생성 생략 - 본인 게시글 좋아요, postId={}, actorUserId={}", postId, actorUserId)
            return
        }

        val alreadyNotified = notificationRepository.existsByUserIdAndActorUserIdAndPostIdAndType(
            postOwnerId,
            actorUserId,
            postId,
            "LIKE",
        )

        if (alreadyNotified) {
            log.info(
                "좋아요 알림 생성 생략 - 이미 생성된 알림, receiverUserId={}, actorUserId={}, postId={}",
                postOwnerId,
                actorUserId,
                postId,
            )
            return
        }

        val actorNickname = findMemberNickname(actorUserId)

        saveNotification(
            receiverUserId = postOwnerId,
            actorUserId = actorUserId,
            postId = postId,
            commentId = null,
            type = "LIKE",
            message = "${actorNickname}님이 회원님의 게시글을 좋아합니다.",
        )

        log.info(
            "좋아요 알림 생성 완료 - receiverUserId={}, actorUserId={}, postId={}",
            postOwnerId,
            actorUserId,
            postId,
        )
    }

    /**
     * 게시글 북마크 알림 생성
     *
     * 주의 사항
     * - 자기 자신의 게시글을 북마크한 경우 알림을 만들지 않음
     * - 같은 사용자가 같은 게시글을 북마크 취소 후 다시 눌러도
     * BOOKMARK 알림은 한 번만 남기도록 중복 생성 방지 검사를 수행
     */
    @Transactional
    fun createBookmarkNotification(postId: Long, actorUserId: Long) {
        log.info("북마크 알림 생성 시작 - postId={}, actorUserId={}", postId, actorUserId)
        val postOwnerId = findPostOwnerId(postId)

        if (postOwnerId == actorUserId) {
            log.info("북마크 알림 생성 생략 - 본인 게시글 북마크, postId={}, actorUserId={}", postId, actorUserId)
            return
        }

        val alreadyNotified = notificationRepository.existsByUserIdAndActorUserIdAndPostIdAndType(
            postOwnerId,
            actorUserId,
            postId,
            "BOOKMARK",
        )

        if (alreadyNotified) {
            log.info(
                "북마크 알림 생성 생략 - 이미 생성된 알림, receiverUserId={}, actorUserId={}, postId={}",
                postOwnerId,
                actorUserId,
                postId,
            )
            return
        }

        val actorNickname = findMemberNickname(actorUserId)

        saveNotification(
            receiverUserId = postOwnerId,
            actorUserId = actorUserId,
            postId = postId,
            commentId = null,
            type = "BOOKMARK",
            message = "${actorNickname}님이 회원님의 게시글을 북마크했습니다.",
        )

        log.info(
            "북마크 알림 생성 완료 - receiverUserId={}, actorUserId={}, postId={}",
            postOwnerId,
            actorUserId,
            postId,
        )
    }

    /**
     * 관리자 처리 후 게시글 신고 결과 알림 생성
     *
     * 주의 사항
     * - 관리자 처리 후에만 신고 대상 게시글 작성자에게 REPORT 알림을 생성한다.
     * - 알림 메시지에는 신고한 사용자를 노출하지 않는다.
     * - 관리자 처리 완료 시점마다 대상 게시글 작성자에게 REPORT 알림을 생성한다.
     */
    @Transactional
    fun createPostReportNotification(postId: Long, adminUserId: Long) {
        log.info("게시글 신고 알림 생성 시작 - postId={}, adminUserId={}", postId, adminUserId)
        val postOwnerId = findPostOwnerId(postId)

        if (postOwnerId == adminUserId) {
            log.info(
                "게시글 신고 알림 생성 생략 - 관리자와 게시글 작성자가 동일, postId={}, adminUserId={}",
                postId,
                adminUserId,
            )
            return
        }

        saveNotification(
            receiverUserId = postOwnerId,
            actorUserId = adminUserId,
            postId = postId,
            commentId = null,
            type = "REPORT",
            message = "회원님의 게시글이 신고 접수되어 관리자에 의해 처리되었습니다.",
        )

        log.info(
            "게시글 신고 알림 생성 완료 - receiverUserId={}, adminUserId={}, postId={}",
            postOwnerId,
            adminUserId,
            postId,
        )
    }

    /**
     * 관리자 처리 후 댓글 신고 결과 알림 생성
     *
     * 주의 사항
     * - 관리자 처리 후에만 신고 대상 댓글 작성자에게 REPORT 알림을 생성한다.
     * - 알림 메시지에는 신고한 사용자를 노출하지 않는다.
     * - 관리자 처리 완료 시점마다 대상 댓글 작성자에게 REPORT 알림을 생성한다.
     */
    @Transactional
    fun createCommentReportNotification(commentId: Long, adminUserId: Long) {
        log.info("댓글 신고 알림 생성 시작 - commentId={}, adminUserId={}", commentId, adminUserId)
        val commentOwnerId = findCommentOwnerId(commentId)

        if (commentOwnerId == adminUserId) {
            log.info(
                "댓글 신고 알림 생성 생략 - 관리자와 댓글 작성자가 동일, commentId={}, adminUserId={}",
                commentId,
                adminUserId,
            )
            return
        }

        saveNotification(
            receiverUserId = commentOwnerId,
            actorUserId = adminUserId,
            postId = null,
            commentId = commentId,
            type = "REPORT",
            message = "회원님의 댓글이 신고 접수되어 관리자에 의해 처리되었습니다.",
        )

        log.info(
            "댓글 신고 알림 생성 완료 - receiverUserId={}, adminUserId={}, commentId={}",
            commentOwnerId,
            adminUserId,
            commentId,
        )
    }

    /**
     * 기존 NotificationService 인터페이스와의 호환을 위한 공통 신고 알림 생성 메서드.
     *
     * 현재 프로젝트에서는 게시글 신고/댓글 신고를 각각 분리해서 사용하고 있지만,
     * 기존 인터페이스에 남아 있는 createReportNotification(...)도 구현해 두어 컴파일 오류가 나지 않도록 맞춘다.
     */
    @Transactional
    fun createReportNotification(targetId: Long?, actorUserId: Long, receiverUserId: Long, message: String) {
        log.info(
            "공통 신고 알림 생성 시작 - targetId={}, actorUserId={}, receiverUserId={}",
            targetId,
            actorUserId,
            receiverUserId,
        )
        if (receiverUserId == actorUserId) {
            log.info(
                "공통 신고 알림 생성 생략 - 수신자와 발생자가 동일, targetId={}, actorUserId={}",
                targetId,
                actorUserId,
            )
            return
        }

        saveNotification(
            receiverUserId = receiverUserId,
            actorUserId = actorUserId,
            postId = null,
            commentId = null,
            type = "REPORT",
            message = message,
        )

        log.info(
            "공통 신고 알림 생성 완료 - targetId={}, actorUserId={}, receiverUserId={}",
            targetId,
            actorUserId,
            receiverUserId,
        )
    }

    /**
     * 현재 로그인한 사용자의 알림 목록 조회 (탭별 페이징 지원)
     *
     * tab 값
     * - all      : 전체 알림
     * - comments : 댓글/대댓글 알림
     * - likes    : 좋아요 알림
     */
    fun getMyNotifications(loginUserId: Long, page: Int, size: Int, tab: String?): NotificationListResponse {
        log.info(
            "알림 목록 조회 시작 - loginUserId={}, page={}, size={}, tab={}",
            loginUserId,
            page,
            size,
            tab,
        )
        val safePage = max(page, 0)
        val safeSize = min(max(size, 1), 50)
        if (safePage != page || safeSize != size) {
            log.info(
                "알림 목록 조회 요청값 보정 - loginUserId={}, requestedPage={}, requestedSize={}, safePage={}, safeSize={}",
                loginUserId,
                page,
                size,
                safePage,
                safeSize,
            )
        }
        val pageable: Pageable = PageRequest.of(safePage, safeSize)

        val types = resolveNotificationTypes(tab)
        log.debug("알림 목록 조회 타입 결정 - loginUserId={}, tab={}, types={}", loginUserId, tab, types)

        val notificationPage = if (types.isEmpty()) {
            notificationRepository.findAvailableByUserIdOrderByCreatedAtDesc(loginUserId, pageable)
        } else {
            notificationRepository.findAvailableByUserIdAndTypeInOrderByCreatedAtDesc(
                loginUserId,
                types,
                pageable,
            )
        }

        val notifications = notificationPage.content
            .map { notification -> toResponse(notification) }

        log.info(
            "알림 목록 조회 완료 - loginUserId={}, tab={}, count={}, totalElements={}, totalPages={}, hasNext={}",
            loginUserId,
            tab,
            notifications.size,
            notificationPage.totalElements,
            notificationPage.totalPages,
            notificationPage.hasNext(),
        )

        return NotificationListResponse(
            notifications,
            notificationPage.number,
            notificationPage.size,
            notificationPage.totalElements,
            notificationPage.totalPages,
            notificationPage.hasNext(),
        )
    }

    /**
     * 특정 알림 읽음 처리
     *
     * 본인 알림만 읽음 처리할 수 있도록 receiver(userId)를 한 번 더 검증
     * 즉, notificationId만 안다고 해서 다른 사용자의 알림을 읽음 처리할 수는 없음
     */
    @Transactional
    fun readNotification(notificationId: Long, loginUserId: Long): NotificationResponse {
        log.info("알림 읽음 처리 시작 - notificationId={}, loginUserId={}", notificationId, loginUserId)
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ApiException(NotificationErrorCode.NOTIFICATION_404_NOT_FOUND) }

        if (notification.userId != loginUserId) {
            log.warn(
                "알림 읽음 처리 거부 - 소유자 불일치, notificationId={}, loginUserId={}, ownerUserId={}",
                notificationId,
                loginUserId,
                notification.userId,
            )
            throw ApiException(NotificationErrorCode.NOTIFICATION_403_FORBIDDEN)
        }

        // 삭제된 게시글과 연결된 알림은 읽음/클릭 처리되지 않도록 차단
        if (!isNotificationTargetAvailable(notification)) {
            log.warn(
                "알림 읽음 처리 거부 - 대상 게시글 사용 불가, notificationId={}, loginUserId={}, postId={}",
                notificationId,
                loginUserId,
                notification.postId,
            )
            throw ApiException(NotificationErrorCode.NOTIFICATION_404_POST_NOT_FOUND)
        }

        notification.markAsRead()
        log.info("알림 읽음 처리 완료 - notificationId={}, loginUserId={}", notificationId, loginUserId)
        return toResponse(notification)
    }

    // 게시글 작성자를 조회해 알림 수신자(receiver)를 구하는 공통 메서드
    private fun findPostOwnerId(postId: Long): Long {
        val post = postRepository.findById(postId)
            .orElseThrow { ApiException(NotificationErrorCode.NOTIFICATION_404_POST_NOT_FOUND) }

        return requireNotNull(post.member.userId)
    }

    // 댓글 작성자를 조회해 신고 알림 수신자(receiver)를 구하는 공통 메서드
    private fun findCommentOwnerId(commentId: Long): Long {
        return commentRepository.findById(commentId)
            .orElseThrow { ApiException(NotificationErrorCode.NOTIFICATION_404_COMMENT_NOT_FOUND) }
            .getUserId()
    }

    // 댓글 조회 공통 메서드. 답글 알림 생성 시 부모 댓글 검증에 사용
    private fun findCommentOrThrow(commentId: Long): Comment {
        return commentRepository.findById(commentId)
            .orElseThrow { ApiException(NotificationErrorCode.NOTIFICATION_404_PARENT_COMMENT_NOT_FOUND) }
    }

    /**
     * 알림 공통 저장 메서드
     *
     * 각 알림 생성 메서드는 receiver / actor / type / message를 결정하는 역할에 집중하고,
     * 실제 Notification 엔티티 생성 및 저장은 여기서 공통 처리
     */
    private fun saveNotification(
        receiverUserId: Long,
        actorUserId: Long,
        postId: Long?,
        commentId: Long?,
        type: String,
        message: String,
    ) {
        val notification = Notification.create(
            userId = receiverUserId,
            actorUserId = actorUserId,
            postId = postId,
            commentId = commentId,
            type = type,
            message = message,
        )

        notificationRepository.save(notification)
        log.debug(
            "알림 저장 완료 - receiverUserId={}, actorUserId={}, postId={}, commentId={}, type={}",
            receiverUserId,
            actorUserId,
            postId,
            commentId,
            type,
        )
    }

    // actorUserId로 회원 닉네임을 조회하는 공통 메서드
    private fun findMemberNickname(userId: Long): String {
        val member = memberRepository.findById(userId)
            .orElseThrow { ApiException(NotificationErrorCode.NOTIFICATION_404_MEMBER_NOT_FOUND) }

        return requireNotNull(member.nickname)
    }

    // 탭 값에 따라 조회할 알림 타입을 결정
    private fun resolveNotificationTypes(tab: String?): List<String> {
        if (tab.isNullOrBlank() || tab.equals("all", ignoreCase = true)) {
            return emptyList()
        }

        if (tab.equals("comments", ignoreCase = true)) {
            return listOf("COMMENT", "REPLY")
        }

        if (tab.equals("likes", ignoreCase = true)) {
            return listOf("LIKE")
        }

        return emptyList()
    }

    // 삭제된 게시글과 연결된 알림인지 확인하는 공통 메서드
    private fun isNotificationTargetAvailable(notification: Notification): Boolean {
        val postId = notification.postId

        // 게시글과 직접 연결되지 않은 알림은 기존처럼 노출
        if (postId == null) {
            return true
        }

        val available = postRepository.findById(postId)
            .map { post -> !post.isDeleted }
            .orElse(false)

        if (!available) {
            log.warn("알림 대상 게시글 사용 불가 - notificationId={}, postId={}", notification.id, postId)
        }

        return available
    }

    // Entity -> Response DTO 변환 메서드
    private fun toResponse(notification: Notification): NotificationResponse {
        val actorUserId = requireNotNull(notification.actorUserId)
        val actorNickname = findMemberNickname(actorUserId)

        return NotificationResponse(
            requireNotNull(notification.id),
            requireNotNull(notification.userId),
            actorUserId,
            actorNickname,
            notification.postId,
            notification.commentId,
            requireNotNull(notification.type),
            requireNotNull(notification.message),
            notification.isRead,
            notification.getCreatedAt(),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(NotificationService::class.java)
    }
}