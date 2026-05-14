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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        notify(targetType, targetId, admin);
        deleteTarget(targetType, targetId);


        if (sanctionType != null) {
            applySanction(targetType, targetId, sanctionType, suspensionDays);
        }
    }

    /* =========================
       NOTIFY
    ========================= */
    private void notify(TargetType targetType, Long targetId, Member admin) {

        if (targetType == TargetType.POST) {
            notificationService.createPostReportNotification(targetId, admin.getUserId());
        }

        if (targetType == TargetType.COMMENT) {
            notificationService.createCommentReportNotification(targetId, admin.getUserId());
        }
    }

    /* =========================
       DELETE
    ========================= */
    private void deleteTarget(TargetType targetType, Long targetId) {

        if (targetType == TargetType.POST) {
            postRepository.findById(targetId)
                    .ifPresent(post -> {
                        if (!post.isDeleted()) post.delete();
                    });
        }

        if (targetType == TargetType.COMMENT) {
            commentRepository.findById(targetId)
                    .ifPresent(comment -> {
                        if (!comment.isDeleted()) comment.softDelete();
                    });
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

        Member member = findTargetMember(targetType, targetId);
        if (member == null) return;

        MemberStatus status = switch (sanctionType) {
            case WARNED -> MemberStatus.WARNED;
            case SUSPENDED -> MemberStatus.SUSPENDED;
            case BLACKLISTED -> MemberStatus.BLACKLISTED;
        };

        memberSanctionService.apply(member, status, suspensionDays);
    }

    private Member findTargetMember(TargetType targetType, Long targetId) {

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

        if (targetType == TargetType.POST) {
            Post post = postRepository.findById(targetId).orElse(null);
            if (post == null) return new TargetInfo(null, null, null);

            return new TargetInfo(
                    post.getMember().getNickname(),
                    post.getTitle(),
                    post.getContent()
            );
        }

        if (targetType == TargetType.COMMENT) {
            Comment comment = commentRepository.findById(targetId).orElse(null);
            if (comment == null) return new TargetInfo(null, null, null);

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
        notify(targetType, targetId, admin);
    }

    public record TargetInfo(
            String nickname,
            String title,
            String content
    ) {
    }

    @Transactional(readOnly = true)
    public boolean exists(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            default -> false;
        };
    }
}