package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO;
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO;
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO;
import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.interaction.report.util.ReportTargetHandler;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import com.back.devc.global.exception.errorCode.ReportErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;

    // batch 조회를 위해 직접 사용
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    private final ReportTargetHandler reportTargetHandler;

    /* =========================================================
     * 1. 단건 신고 조회 (기존 유지 가능하지만 N+1 있음)
     * ========================================================= */
    @Transactional(readOnly = true)
    public Page<ReportResponseDTO> getReports(ReportStatus status, Pageable pageable) {

        Page<Report> reports = (status == null)
                ? reportRepository.findAll(pageable)
                : reportRepository.findAllByStatus(status, pageable);

        // 여기까지는 N+1 가능성 남아있음 (원하면 이것도 batch 방식으로 개선 가능)
        return reports.map(reportTargetHandler::toDtoWithTargetInfo);
    }

    /* =========================================================
     * 2. 그룹 조회 no batch
     * ========================================================= */
    @Transactional(readOnly = true)
    public Page<ReportGroupResponseDTO> getGroupedReportsNoBatch(ReportStatus status, Pageable pageable) {

        Page<Object[]> result = reportRepository.findGroupedReports(status, pageable);

        return result.map(row -> {

            TargetType targetType = (TargetType) row[0];
            Long targetId = (Long) row[1];
            Long reportCount = (Long) row[2];
            LocalDateTime latestCreatedAt = (LocalDateTime) row[3];

            // target 정보 조회 (중복 제거)
            ReportTargetHandler.TargetInfo info =
                    reportTargetHandler.getTargetInfo(targetType, targetId);

            // reasonTypes 조회
            List<String> reasonTypes =
                    reportRepository.findReasonTypesByTargetId(targetType, targetId);

            return new ReportGroupResponseDTO(
                    targetType,
                    targetId,
                    info.nickname(),
                    info.title(),
                    info.content(),
                    reportCount,
                    reasonTypes,
                    status,
                    latestCreatedAt
            );
        });
    }


    /* =========================================================
     * 2. 그룹 조회 (IN batch 방식으로 N+1 제거)
     * ========================================================= */
    @Transactional(readOnly = true)
    public Page<ReportGroupResponseDTO> getGroupedReports(ReportStatus status, Pageable pageable) {

        Page<Object[]> result = reportRepository.findGroupedReports(status, pageable);

        // 1) row에서 targetType/targetId 추출
        List<Object[]> rows = result.getContent();

        List<Long> postIds = new ArrayList<>();
        List<Long> commentIds = new ArrayList<>();

        for (Object[] row : rows) {
            TargetType targetType = (TargetType) row[0];
            Long targetId = (Long) row[1];

            if (targetType == TargetType.POST) postIds.add(targetId);
            if (targetType == TargetType.COMMENT) commentIds.add(targetId);
        }

        // 2) Post, Comment 한번에 조회
        Map<Long, Post> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : postRepository.findAllByPostIdIn(postIds)
                .stream()
                .collect(Collectors.toMap(Post::getPostId, Function.identity()));

        Map<Long, Comment> commentMap = commentIds.isEmpty()
                ? Collections.emptyMap()
                : commentRepository.findAllByIdIn(commentIds)
                .stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));

        // 3) COMMENT 작성자 userId 목록 batch 조회
        List<Long> commentWriterIds = commentMap.values().stream()
                .map(Comment::getUserId)
                .distinct()
                .toList();

        Map<Long, Member> memberMap = commentWriterIds.isEmpty()
                ? Collections.emptyMap()
                : memberRepository.findAllByUserIdIn(commentWriterIds)
                .stream()
                .collect(Collectors.toMap(Member::getUserId, Function.identity()));

        // 4) reasonTypes batch 조회
        Map<String, List<String>> reasonTypeMap = loadReasonTypesBatch(postIds, commentIds);

        // 5) row -> DTO 변환
        Page<ReportGroupResponseDTO> dtoPage = result.map(row -> {
            TargetType targetType = (TargetType) row[0];
            Long targetId = (Long) row[1];
            Long reportCount = (Long) row[2];
            LocalDateTime latestCreatedAt = (LocalDateTime) row[3];

            ReportTargetHandler.TargetInfo info = resolveTargetInfo(targetType, targetId, postMap, commentMap, memberMap);

            String key = buildKey(targetType, targetId);
            List<String> reasonTypes = reasonTypeMap.getOrDefault(key, List.of());

            return new ReportGroupResponseDTO(
                    targetType,
                    targetId,
                    info.nickname(),
                    info.title(),
                    info.content(),
                    reportCount,
                    reasonTypes,
                    status,
                    latestCreatedAt
            );
        });

        return dtoPage;
    }

    private Map<String, List<String>> loadReasonTypesBatch(List<Long> postIds, List<Long> commentIds) {

        if (postIds.isEmpty() && commentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> reasonRows = reportRepository.findReasonTypesBatch(
                TargetType.POST,
                postIds.isEmpty() ? List.of(-1L) : postIds,
                TargetType.COMMENT,
                commentIds.isEmpty() ? List.of(-1L) : commentIds
        );

        Map<String, List<String>> map = new HashMap<>();

        for (Object[] row : reasonRows) {
            TargetType type = (TargetType) row[0];
            Long id = (Long) row[1];
            String reasonType = (String) row[2];

            String key = buildKey(type, id);

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(reasonType);
        }

        return map;
    }

    private String buildKey(TargetType type, Long id) {
        return type.name() + ":" + id;
    }

    private ReportTargetHandler.TargetInfo resolveTargetInfo(
            TargetType targetType,
            Long targetId,
            Map<Long, Post> postMap,
            Map<Long, Comment> commentMap,
            Map<Long, Member> memberMap
    ) {

        if (targetType == TargetType.POST) {
            Post post = postMap.get(targetId);
            if (post == null) return new ReportTargetHandler.TargetInfo(null, null, null);

            return new ReportTargetHandler.TargetInfo(
                    post.getMember().getNickname(),
                    post.getTitle(),
                    post.getContent()
            );
        }

        if (targetType == TargetType.COMMENT) {
            Comment comment = commentMap.get(targetId);
            if (comment == null) return new ReportTargetHandler.TargetInfo(null, null, null);

            Member member = memberMap.get(comment.getUserId());

            return new ReportTargetHandler.TargetInfo(
                    member != null ? member.getNickname() : null,
                    null,
                    comment.getContent()
            );
        }

        return new ReportTargetHandler.TargetInfo(null, null, null);
    }

    /* =========================================================
     * 3. 단건 승인
     * ========================================================= */
    public void approveReport(Long adminId, AdminReportRequestDTO dto) {
        Member admin = findMemberOrThrow(adminId);
        validateAdminRole(admin);

        Report report = findReportOrThrow(dto.reportId());
        validatePendingStatus(report);
        validateTargetExists(report.getTargetType(), report.getTargetId());
        validateSanctionDetails(dto);

        report.processReport(admin);

        reportTargetHandler.handleApproved(
                report.getTargetType(),
                report.getTargetId(),
                admin,
                dto.sanctionType(),
                dto.suspensionDays()
        );
    }

    /* =========================================================
     * 4. 단건 반려
     * ========================================================= */
    public void rejectReport(Long adminId, AdminReportRequestDTO dto) {
        Member admin = findMemberOrThrow(adminId);
        validateAdminRole(admin);

        Report report = findReportOrThrow(dto.reportId());
        validatePendingStatus(report);

        report.rejectReport(admin);

        reportTargetHandler.handleRejected(
                report.getTargetType(),
                report.getTargetId(),
                admin
        );
    }

    /* =========================================================
     * 5. 그룹 승인
     * ========================================================= */
    @Transactional // 반드시 트랜잭션 안에서 실행
    public void approveReportGroup(Long adminId, AdminReportRequestDTO dto) {
        Member admin = findMemberOrThrow(adminId);
        validateAdminRole(admin);

        TargetType targetType = dto.targetType();
        Long targetId = dto.reportId();

        validateTargetExists(targetType, targetId);
        validateSanctionDetails(dto);

        //  'PENDING'인 것만 한 번에 'RESOLVE'로 변경
        int updatedCount = reportRepository.updateStatusGroup(
                targetType, targetId, admin, ReportStatus.RESOLVED, ReportStatus.PENDING
        );

        // 수정된 행이 0개라면? 이미 다른 관리자가 처리한 상태
        if (updatedCount == 0) {
            throw new ApiException(ReportErrorCode.REPORT_404_PENDING_LIST);

        }

        // 업데이트에 성공(updatedCount > 0)한 경우에만 제재 로직 실행
        reportTargetHandler.handleApproved(
                targetType,
                targetId,
                admin,
                dto.sanctionType(),
                dto.suspensionDays()
        );
    }

    /* =========================================================
     * 6. 그룹 반려
     * ========================================================= */
    @Transactional
    public void rejectReportGroup(Long adminId, AdminReportRequestDTO dto) {
        Member admin = findMemberOrThrow(adminId);
        validateAdminRole(admin);

        TargetType targetType = dto.targetType();
        Long targetId = dto.reportId();

        // 반려 역시 'PENDING' 상태인 것만 'REJECT'로 일괄 변경
        int updatedCount = reportRepository.updateStatusGroup(
                targetType, targetId, admin, ReportStatus.REJECTED, ReportStatus.PENDING
        );

        if (updatedCount == 0) {
            throw new ApiException(ReportErrorCode.REPORT_404_PENDING_LIST);
        }

        reportTargetHandler.handleRejected(targetType, targetId, admin);
    }

    /* =========================================================
     * Util
     * ========================================================= */
    private void validateAdminRole(Member member) {
        if (!member.isAdmin()) {
            throw new ApiException(ReportErrorCode.REPORT_403_UNAUTHORIZED_ADMIN);
        }
    }

    private void validateTargetExists(TargetType type, Long targetId) {
        if (!reportTargetHandler.exists(type, targetId)) {
            throw new ApiException(ReportErrorCode.REPORT_404_TARGET);
        }
    }

    private void validateSanctionDetails(AdminReportRequestDTO dto) {
        if (SanctionType.SUSPENDED.equals(dto.sanctionType()) &&
                (dto.suspensionDays() == null || dto.suspensionDays() <= 0)) {
            throw new ApiException(ReportErrorCode.REPORT_400_INVALID_SANCTION_PARAMETER);
        }
    }

    private void validatePendingStatus(Report report) {
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ApiException(ReportErrorCode.REPORT_409_ALREADY_REPORT);
        }
    }

    private Report findReportOrThrow(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(ReportErrorCode.REPORT_404_REPORT));
    }

    private Member findMemberOrThrow(Long userId) {
        return memberRepository.findById(userId)
                .orElseThrow(() -> new ApiException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}