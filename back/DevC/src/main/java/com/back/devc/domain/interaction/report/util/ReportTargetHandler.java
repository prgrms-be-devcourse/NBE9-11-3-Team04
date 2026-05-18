package com.back.devc.domain.interaction.report.util;

import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO;
import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.member.service.MemberSanctionService;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.ReportErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ReportTargetHandler {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final MemberSanctionService memberSanctionService;

    public void handleApproved(
            TargetType targetType,
            Long targetId,
            Member admin,
            SanctionType sanctionType,
            Integer suspensionDays
    ) {
        log.info("신고 승인 대상 처리 시작 - targetType={}, targetId={}, adminId={}, sanctionType={}, suspensionDays={}",
                targetType,
                targetId,
                admin.getUserId(),
                sanctionType,
                suspensionDays);

        notify(targetType, targetId, admin);
        log.info("신고 승인 알림 처리 완료 - targetType={}, targetId={}, adminId={}", targetType, targetId, admin.getUserId());

        deleteTarget(targetType, targetId);
        log.info("신고 승인 대상 삭제 처리 완료 - targetType={}, targetId={}", targetType, targetId);

        if (sanctionType != null) {
            applySanction(targetType, targetId, sanctionType, suspensionDays);
            log.info("신고 승인 제재 처리 완료 - targetType={}, targetId={}, sanctionType={}, suspensionDays={}",
                    targetType,
                    targetId,
                    sanctionType,
                    suspensionDays);
        } else {
            log.info("신고 승인 제재 처리 생략 - 제재 타입 없음, targetType={}, targetId={}", targetType, targetId);
        }

        log.info("신고 승인 대상 처리 완료 - targetType={}, targetId={}, adminId={}", targetType, targetId, admin.getUserId());
    }

    /* =========================
       NOTIFY
    ========================= */
    private void notify(TargetType targetType, Long targetId, Member admin) {
        log.info("신고 처리 알림 생성 요청 - targetType={}, targetId={}, adminId={}", targetType, targetId, admin.getUserId());

        if (targetType == TargetType.POST) {
            notificationService.createPostReportNotification(targetId, admin.getUserId());
            log.info("게시글 신고 처리 알림 생성 요청 완료 - postId={}, adminId={}", targetId, admin.getUserId());
        }

        if (targetType == TargetType.COMMENT) {
            notificationService.createCommentReportNotification(targetId, admin.getUserId());
            log.info("댓글 신고 처리 알림 생성 요청 완료 - commentId={}, adminId={}", targetId, admin.getUserId());
        }
    }

    /* =========================
       DELETE
    ========================= */
    private void deleteTarget(TargetType targetType, Long targetId) {
        log.info("신고 대상 삭제 처리 시작 - targetType={}, targetId={}", targetType, targetId);

        if (targetType == TargetType.POST) {
            postRepository.findById(targetId)
                    .ifPresentOrElse(post -> {
                        if (!post.getIsDeleted()) {
                            post.delete();
                            log.info("신고 대상 게시글 삭제 처리 완료 - postId={}", targetId);
                        } else {
                            log.info("신고 대상 게시글 삭제 처리 생략 - 이미 삭제됨, postId={}", targetId);
                        }
                    }, () -> log.warn("신고 대상 게시글 삭제 처리 실패 - 게시글 없음, postId={}", targetId));
        }

        if (targetType == TargetType.COMMENT) {
            commentRepository.findById(targetId)
                    .ifPresentOrElse(comment -> {
                        if (!comment.isDeleted()) {
                            comment.softDelete();
                            log.info("신고 대상 댓글 삭제 처리 완료 - commentId={}", targetId);
                        } else {
                            log.info("신고 대상 댓글 삭제 처리 생략 - 이미 삭제됨, commentId={}", targetId);
                        }
                    }, () -> log.warn("신고 대상 댓글 삭제 처리 실패 - 댓글 없음, commentId={}", targetId));
        }
    }

    /* =========================
       SANCTION
    ========================= */
    private void applySanction(
            TargetType targetType,
            Long targetId,
            SanctionType sanctionType,
            Integer suspensionDays
    ) {
        log.info("신고 대상 작성자 제재 처리 시작 - targetType={}, targetId={}, sanctionType={}, suspensionDays={}",
                targetType,
                targetId,
                sanctionType,
                suspensionDays);

        Member member = findTargetMember(targetType, targetId);
        if (member == null) {
            log.warn("신고 대상 작성자 제재 처리 생략 - 대상 작성자 없음, targetType={}, targetId={}", targetType, targetId);
            return;
        }

        MemberStatus status = switch (sanctionType) {
            case WARNED -> MemberStatus.WARNED;
            case SUSPENDED -> MemberStatus.SUSPENDED;
            case BLACKLISTED -> MemberStatus.BLACKLISTED;
        };

        log.info("신고 대상 작성자 제재 상태 결정 - targetType={}, targetId={}, targetUserId={}, sanctionType={}, memberStatus={}, suspensionDays={}",
                targetType,
                targetId,
                member.getUserId(),
                sanctionType,
                status,
                suspensionDays);

        memberSanctionService.apply(member, status, suspensionDays);

        log.info("신고 대상 작성자 제재 처리 완료 - targetType={}, targetId={}, targetUserId={}, memberStatus={}, suspensionDays={}",
                targetType,
                targetId,
                member.getUserId(),
                status,
                suspensionDays);
    }

    private Member findTargetMember(TargetType targetType, Long targetId) {
        log.debug("신고 대상 작성자 조회 시작 - targetType={}, targetId={}", targetType, targetId);

        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(Post::getMember)
                    .orElseThrow(() -> new ApiException(ReportErrorCode.REPORT_404_TARGET_USER)); // [에러] 신고 대상 포스트가 없음

            case COMMENT -> commentRepository.findById(targetId)
                    .map(Comment::getUserId)
                    .flatMap(memberRepository::findById)
                    .orElseThrow(() -> new ApiException(ReportErrorCode.REPORT_404_TARGET_USER)); // [에러] 신고 대상 댓글 혹은 작성자가 없음        };
        };
    }


    @Transactional(readOnly = true)
    public ReportResponseDTO toDtoWithTargetInfo(Report report) {

        TargetInfo info = getTargetInfo(
                report.getTargetType(),
                report.getTargetId()
        );

        return ReportResponseDTO.of(
                report,
                info.nickname(),
                info.title(),
                info.content()
        );
    }

    public TargetInfo getTargetInfo(TargetType targetType, Long targetId) {
        log.debug("신고 대상 정보 조회 시작 - targetType={}, targetId={}", targetType, targetId);

        if (targetType == TargetType.POST) {
            Post post = postRepository.findById(targetId).orElse(null);
            if (post == null) {
                log.warn("신고 대상 게시글 정보 조회 실패 - postId={}", targetId);
                return new TargetInfo(null, null, null);
            }

            return new TargetInfo(
                    post.member.getNickname(),
                    post.title,
                    post.content
            );
        }

        if (targetType == TargetType.COMMENT) {
            Comment comment = commentRepository.findById(targetId).orElse(null);
            if (comment == null) {
                log.warn("신고 대상 댓글 정보 조회 실패 - commentId={}", targetId);
                return new TargetInfo(null, null, null);
            }

            String nickname = memberRepository.findById(comment.getUserId())
                    .map(Member::getNickname)
                    .orElse(null);

            return new TargetInfo(
                    nickname,
                    null,
                    comment.getContent()
            );
        }

        return new TargetInfo(null, null, null);
    }

    public void handleRejected(TargetType targetType, Long targetId, Member admin) {
        log.info("신고 반려 대상 처리 시작 - targetType={}, targetId={}, adminId={}", targetType, targetId, admin.getUserId());
        notify(targetType, targetId, admin);
        log.info("신고 반려 대상 처리 완료 - targetType={}, targetId={}, adminId={}", targetType, targetId, admin.getUserId());
    }

    public record TargetInfo(
            String nickname,
            String title,
            String content
    ) {
    }

    @Transactional(readOnly = true)
    public boolean exists(TargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            default -> false;
        };

        if (!exists) {
            log.warn("신고 대상 존재 여부 확인 실패 - targetType={}, targetId={}", targetType, targetId);
        } else {
            log.debug("신고 대상 존재 여부 확인 완료 - targetType={}, targetId={}", targetType, targetId);
        }

        return exists;
    }
}