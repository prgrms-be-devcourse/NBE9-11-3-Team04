package com.back.devc.domain.interaction.notification.service;

import com.back.devc.domain.interaction.notification.dto.NotificationListResponse;
import com.back.devc.domain.interaction.notification.dto.NotificationResponse;
import com.back.devc.domain.interaction.notification.entity.Notification;
import com.back.devc.domain.interaction.notification.repository.NotificationRepository;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.comment.service.CommentService.CommentCreatedEvent;
import com.back.devc.domain.post.comment.service.CommentService.ReplyCreatedEvent;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

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
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    // 알림 저장/조회에 사용하는 JPA Repository.
    private final NotificationRepository notificationRepository;

    // 답글 알림 생성 시 부모 댓글 상태(존재 여부, 삭제 여부, 작성자)를 확인할 때 사용
    private final CommentRepository commentRepository;

    // actorUserId로 회원 닉네임을 조회해 알림 메시지/응답에 사용
    private final MemberRepository memberRepository;

    // 게시글 작성자(userId)를 찾아 "알림 수신자"를 결정할 때 사용
    private final PostRepository postRepository;

    /**
     * 댓글 저장 트랜잭션이 정상 커밋된 이후 댓글 알림을 생성한다.
     * 알림 생성에 실패해도 이미 커밋된 댓글 작성 결과에는 영향을 주지 않도록 예외를 삼킨다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        try {
            createCommentNotification(event.postId(), event.actorUserId(), event.commentId());
        } catch (Exception e) {
            log.warn("댓글 알림 생성 실패 - postId={}, actorUserId={}, commentId={}",
                    event.postId(), event.actorUserId(), event.commentId(), e);
        }
    }

    /**
     * 대댓글 저장 트랜잭션이 정상 커밋된 이후 대댓글 알림을 생성한다.
     * 알림 생성에 실패해도 이미 커밋된 대댓글 작성 결과에는 영향을 주지 않도록 예외를 삼킨다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReplyCreatedEvent(ReplyCreatedEvent event) {
        try {
            createReplyNotification(event.parentCommentId(), event.actorUserId(), event.replyCommentId());
        } catch (Exception e) {
            log.warn("대댓글 알림 생성 실패 - parentCommentId={}, actorUserId={}, replyCommentId={}",
                    event.parentCommentId(), event.actorUserId(), event.replyCommentId(), e);
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
    public void createCommentNotification(Long postId, Long actorUserId, Long commentId) {
        Long postOwnerId = findPostOwnerId(postId);

        if (postOwnerId.equals(actorUserId)) {
            return;
        }

        String actorNickname = findMemberNickname(actorUserId);

        saveNotification(
                postOwnerId,
                actorUserId,
                postId,
                commentId,
                "COMMENT",
                actorNickname + "님이 게시글에 댓글을 남겼습니다."
        );
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
    public void createReplyNotification(Long parentCommentId, Long actorUserId, Long replyCommentId) {
        Comment parentComment = findCommentOrThrow(parentCommentId);

        if (parentComment.isDeleted()) {
            return;
        }

        Long receiverUserId = parentComment.getUserId();

        if (receiverUserId.equals(actorUserId)) {
            return;
        }

        String actorNickname = findMemberNickname(actorUserId);

        saveNotification(
                receiverUserId,
                actorUserId,
                parentComment.getPostId(),
                replyCommentId,
                "REPLY",
                actorNickname + "님이 회원님의 댓글에 답글을 남겼습니다."
        );
    }

    /**
     * 게시글 좋아요 알림 생성
     *
     * 주의 사항
     * - 자기 자신의 게시글에 좋아요를 누른 경우 알림을 만들지 않음
     * - 같은 사용자가 같은 게시글에 대해 좋아요 취소 후 다시 눌러도
     *   LIKE 알림은 한 번만 남기도록 중복 생성 방지 검사를 수행
     */
    @Transactional
    public void createPostLikeNotification(Long postId, Long actorUserId) {
        Long postOwnerId = findPostOwnerId(postId);

        if (postOwnerId.equals(actorUserId)) {
            return;
        }

        boolean alreadyNotified = notificationRepository
                .existsByUserIdAndActorUserIdAndPostIdAndType(postOwnerId, actorUserId, postId, "LIKE");

        if (alreadyNotified) {
            return;
        }

        String actorNickname = findMemberNickname(actorUserId);

        saveNotification(
                postOwnerId,
                actorUserId,
                postId,
                null,
                "LIKE",
                actorNickname + "님이 회원님의 게시글을 좋아합니다."
        );
    }

    /**
     * 게시글 북마크 알림 생성
     *
     * 주의 사항
     * - 자기 자신의 게시글을 북마크한 경우 알림을 만들지 않음
     * - 같은 사용자가 같은 게시글을 북마크 취소 후 다시 눌러도
     *   BOOKMARK 알림은 한 번만 남기도록 중복 생성 방지 검사를 수행
     */
    @Transactional
    public void createBookmarkNotification(Long postId, Long actorUserId) {
        Long postOwnerId = findPostOwnerId(postId);

        if (postOwnerId.equals(actorUserId)) {
            return;
        }

        boolean alreadyNotified = notificationRepository
                .existsByUserIdAndActorUserIdAndPostIdAndType(postOwnerId, actorUserId, postId, "BOOKMARK");

        if (alreadyNotified) {
            return;
        }

        String actorNickname = findMemberNickname(actorUserId);

        saveNotification(
                postOwnerId,
                actorUserId,
                postId,
                null,
                "BOOKMARK",
                actorNickname + "님이 회원님의 게시글을 북마크했습니다."
        );
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
    public void createPostReportNotification(Long postId, Long adminUserId) {
        Long postOwnerId = findPostOwnerId(postId);

        if (postOwnerId.equals(adminUserId)) {
            return;
        }

        saveNotification(
                postOwnerId,
                adminUserId,
                postId,
                null,
                "REPORT",
                "회원님의 게시글이 신고 접수되어 관리자에 의해 처리되었습니다."
        );
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
    public void createCommentReportNotification(Long commentId, Long adminUserId) {
        Long commentOwnerId = findCommentOwnerId(commentId);

        if (commentOwnerId.equals(adminUserId)) {
            return;
        }

        saveNotification(
                commentOwnerId,
                adminUserId,
                null,
                commentId,
                "REPORT",
                "회원님의 댓글이 신고 접수되어 관리자에 의해 처리되었습니다."
        );
    }

    /**
     * 기존 NotificationService 인터페이스와의 호환을 위한 공통 신고 알림 생성 메서드.
     *
     * 현재 프로젝트에서는 게시글 신고/댓글 신고를 각각 분리해서 사용하고 있지만,
     * 기존 인터페이스에 남아 있는 createReportNotification(...)도 구현해 두어 컴파일 오류가 나지 않도록 맞춘다.
     */
    @Transactional
    public void createReportNotification(Long targetId, Long actorUserId, Long receiverUserId, String message) {
        if (receiverUserId.equals(actorUserId)) {
            return;
        }

        saveNotification(
                receiverUserId,
                actorUserId,
                null,
                null,
                "REPORT",
                message
        );
    }

    /**
     * 현재 로그인한 사용자의 알림 목록 조회 (탭별 페이징 지원)
     *
     * tab 값
     * - all      : 전체 알림
     * - comments : 댓글/대댓글 알림
     * - likes    : 좋아요 알림
     */
    public NotificationListResponse getMyNotifications(Long loginUserId, int page, int size, String tab) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        List<String> types = resolveNotificationTypes(tab);

        Page<Notification> notificationPage = types.isEmpty()
                ? notificationRepository.findAvailableByUserIdOrderByCreatedAtDesc(loginUserId, pageable)
                : notificationRepository.findAvailableByUserIdAndTypeInOrderByCreatedAtDesc(
                loginUserId,
                types,
                pageable
        );

        List<NotificationResponse> notifications = notificationPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new NotificationListResponse(
                notifications,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.hasNext()
        );
    }

    /**
     * 특정 알림 읽음 처리
     *
     * 본인 알림만 읽음 처리할 수 있도록 receiver(userId)를 한 번 더 검증
     * 즉, notificationId만 안다고 해서 다른 사용자의 알림을 읽음 처리할 수는 없음
     */
    @Transactional
    public NotificationResponse readNotification(Long notificationId, Long loginUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_404_NOT_FOUND));

        if (!notification.getUserId().equals(loginUserId)) {
            throw new ApiException(NotificationErrorCode.NOTIFICATION_403_FORBIDDEN);
        }

        // 삭제된 게시글과 연결된 알림은 읽음/클릭 처리되지 않도록 차단
        if (!isNotificationTargetAvailable(notification)) {
            throw new ApiException(NotificationErrorCode.NOTIFICATION_404_POST_NOT_FOUND);
        }

        notification.markAsRead();
        return toResponse(notification);
    }

    // 게시글 작성자를 조회해 알림 수신자(receiver)를 구하는 공통 메서드
    private Long findPostOwnerId(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_404_POST_NOT_FOUND))
                .getMember()
                .getUserId();
    }

    // 댓글 작성자를 조회해 신고 알림 수신자(receiver)를 구하는 공통 메서드
    private Long findCommentOwnerId(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_404_COMMENT_NOT_FOUND))
                .getUserId();
    }

    // 댓글 조회 공통 메서드. 답글 알림 생성 시 부모 댓글 검증에 사용
    private Comment findCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_404_PARENT_COMMENT_NOT_FOUND));
    }

    /**
     * 알림 공통 저장 메서드
     *
     * 각 알림 생성 메서드는 receiver / actor / type / message를 결정하는 역할에 집중하고,
     * 실제 Notification 엔티티 생성 및 저장은 여기서 공통 처리
     */
    private void saveNotification(
            Long receiverUserId,
            Long actorUserId,
            Long postId,
            Long commentId,
            String type,
            String message
    ) {
        Notification notification = Notification.create(
                receiverUserId,
                actorUserId,
                postId,
                commentId,
                type,
                message
        );

        notificationRepository.save(notification);
    }

    // actorUserId로 회원 닉네임을 조회하는 공통 메서드
    private String findMemberNickname(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_404_MEMBER_NOT_FOUND))
                .getNickname();
    }

    // 탭 값에 따라 조회할 알림 타입을 결정
    private List<String> resolveNotificationTypes(String tab) {
        if (tab == null || tab.isBlank() || tab.equalsIgnoreCase("all")) {
            return List.of();
        }

        if (tab.equalsIgnoreCase("comments")) {
            return List.of("COMMENT", "REPLY");
        }

        if (tab.equalsIgnoreCase("likes")) {
            return List.of("LIKE");
        }

        return List.of();
    }

    // 삭제된 게시글과 연결된 알림인지 확인하는 공통 메서드
    private boolean isNotificationTargetAvailable(Notification notification) {
        Long postId = notification.getPostId();

        // 게시글과 직접 연결되지 않은 알림은 기존처럼 노출
        if (postId == null) {
            return true;
        }

        return postRepository.findById(postId)
                .map(post -> !post.isDeleted())
                .orElse(false);
    }

    // Entity -> Response DTO 변환 메서드
    private NotificationResponse toResponse(Notification notification) {
        String actorNickname = notification.getActorUserId() != null
                ? findMemberNickname(notification.getActorUserId())
                : null;

        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getActorUserId(),
                actorNickname,
                notification.getPostId(),
                notification.getCommentId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
