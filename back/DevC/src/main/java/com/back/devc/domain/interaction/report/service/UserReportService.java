package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO;
import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.exception.errorCode.ReportErrorCode;
import com.back.devc.global.exception.errorCode.AuthErrorCode;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    /* =========================================================
     *  Public API: 게시글/댓글 신고 엔드포인트
     * ========================================================= */
    public void reportPost(Long reporterId, ReportRequestDTO dto) {
        report(reporterId, TargetType.POST, dto);
    }

    public void reportComment(Long reporterId, ReportRequestDTO dto) {
        report(reporterId, TargetType.COMMENT, dto);
    }

    /* =========================================================
     * 통합 신고 로직
     * ========================================================= */
    public void report(Long reporterId, TargetType targetType, ReportRequestDTO dto) {

        Member reporter = findMemberOrThrow(reporterId);

        validateTarget(reporterId, targetType, dto.targetId());
        validateDuplicateReport(reporter, targetType, dto.targetId());

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(targetType)
                .targetId(dto.targetId())
                .reasonType(dto.reasonType())
                .reasonDetail(dto.reasonDetail())
                .build();

        reportRepository.save(report);

    }

    /* =========================================================
     * Validation
     * ========================================================= */

    private void validateTarget(Long reporterId, TargetType type, Long targetId) {

        if (type == TargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException(ReportErrorCode.REPORT_404_TARGET));

            if (post.getMember().getUserId().equals(reporterId)) {
                throw new ApiException(ReportErrorCode.REPORT_400_REPORT_SELF);
            }

            if (post.isDeleted()) {
                throw new ApiException(ReportErrorCode.REPORT_410_ALREADY_DELETED);
            }
        }

        if (type == TargetType.COMMENT) {
            Comment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new ApiException(ReportErrorCode.REPORT_404_TARGET));

            if (comment.getUserId().equals(reporterId)) {
                throw new ApiException(ReportErrorCode.REPORT_400_REPORT_SELF);
            }

            if (comment.isDeleted()) {
                throw new ApiException(ReportErrorCode.REPORT_410_ALREADY_DELETED);
            }
        }
    }

    private void validateDuplicateReport(Member reporter, TargetType type, Long targetId) {
        if (reportRepository.existsByReporterAndTargetTypeAndTargetId(
                reporter, type, targetId)) {
            throw new ApiException(ReportErrorCode.REPORT_409_ALREADY_REPORT_USER);
        }
    }

    private Member findMemberOrThrow(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new ApiException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}