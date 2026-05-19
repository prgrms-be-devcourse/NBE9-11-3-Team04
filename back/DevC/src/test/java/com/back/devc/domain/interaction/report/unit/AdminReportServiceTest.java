package com.back.devc.domain.interaction.report.unit;

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO;
import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest;
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO;
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO;
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest;
import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportGroup;
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository;
import com.back.devc.domain.interaction.report.repository.ReportReasonStatProjection;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.interaction.report.service.AdminReportService;
import com.back.devc.domain.interaction.report.util.ReportTargetHandler;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.exception.ErrorCodeSpec;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import com.back.devc.global.exception.errorCode.ReportErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminReportService")
class AdminReportServiceTest {

    @InjectMocks
    private AdminReportService adminReportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportGroupRepository reportGroupRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReportTargetHandler reportTargetHandler;

    private Member admin;
    private Member user;

    @BeforeEach
    void setUp() {
        admin = mock(Member.class);
        user = mock(Member.class);

        given(admin.isAdmin()).willReturn(true);
        given(admin.getUserId()).willReturn(1L);
        given(user.isAdmin()).willReturn(false);
    }

    @Nested
    @DisplayName("getReports")
    class GetReports {

        @Test
        @DisplayName("uses findAll when status is null")
        void getReports_usesFindAllWithoutStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Report report = report(ReportStatus.PENDING);
            ReportResponseDTO dto = mock(ReportResponseDTO.class);
            given(reportRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(report), pageable, 1));
            given(reportTargetHandler.toDtoWithTargetInfo(report)).willReturn(dto);

            var result = adminReportService.getReports(null, pageable);

            assertThat(result.getContent()).containsExactly(dto);
            verify(reportRepository).findAll(pageable);
            verify(reportRepository, never()).findAllByStatus(any(), any());
        }

        @Test
        @DisplayName("uses findAllByStatus when status is provided")
        void getReports_usesFindAllByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Report report = report(ReportStatus.PENDING);
            ReportResponseDTO dto = mock(ReportResponseDTO.class);
            given(reportRepository.findAllByStatus(ReportStatus.PENDING, pageable))
                    .willReturn(new PageImpl<>(List.of(report), pageable, 1));
            given(reportTargetHandler.toDtoWithTargetInfo(report)).willReturn(dto);

            var result = adminReportService.getReports(ReportStatus.PENDING, pageable);

            assertThat(result.getContent()).containsExactly(dto);
            verify(reportRepository).findAllByStatus(ReportStatus.PENDING, pageable);
            verify(reportRepository, never()).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("getGroupedReports")
    class GetGroupedReports {
        @Test
        @DisplayName("maps grouped post and comment rows with batch-loaded target information")
        void getGroupedReports_mapsRowsWithBatchTargetInfo() {
            LocalDateTime latestPostReport = LocalDateTime.of(2026, 1, 2, 10, 0);
            LocalDateTime latestCommentReport = LocalDateTime.of(2026, 1, 2, 11, 0);
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 1, 3, 0, 0);
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "latestCreatedAt"));

            Pageable reportGroupPageable =
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "latestReportedAt"));
            ReportGroup postReportGroup =
                    reportGroup(1L, TargetType.POST, 10L, 2, latestPostReport);
            ReportGroup commentReportGroup =
                    reportGroup(2L, TargetType.COMMENT, 20L, 3, latestCommentReport);

            given(reportGroupRepository.findReportGroups(
                    ReportGroupStatus.OPEN,
                    from,
                    to,
                    reportGroupPageable
            )).willReturn(new PageImpl<>(
                    List.of(postReportGroup, commentReportGroup),
                    reportGroupPageable,
                    2
            ));

            Member postAuthor = mock(Member.class);
            given(postAuthor.getNickname()).willReturn("post-writer");
            Post post = mock(Post.class);
            given(post.getPostId()).willReturn(10L);
            given(post.getMember()).willReturn(postAuthor);
            given(post.getTitle()).willReturn("reported post");
            given(post.getContent()).willReturn("post content");
            given(postRepository.findAllByPostIdIn(List.of(10L))).willReturn(List.of(post));

            Comment comment = mock(Comment.class);
            given(comment.getId()).willReturn(20L);
            given(comment.getUserId()).willReturn(30L);
            given(comment.getContent()).willReturn("comment content");
            given(commentRepository.findAllByIdIn(List.of(20L))).willReturn(List.of(comment));

            Member commentAuthor = mock(Member.class);
            given(commentAuthor.getUserId()).willReturn(30L);
            given(commentAuthor.getNickname()).willReturn("comment-writer");

            // 🔧 고쳐야 할 부분: findAllByUserIdIn → findAllById
            given(memberRepository.findAllById(List.of(30L)))
                    .willReturn(List.of(commentAuthor));

            given(reportRepository.findReasonStatsByReportGroupIds(List.of(1L, 2L)))
                    .willReturn(List.of(
                            reasonStat(1L, "SPAM", 1L),
                            reasonStat(2L, "ABUSE", 1L),
                            reasonStat(2L, "HATE", 1L)
                    ));

            var result = adminReportService.getGroupedReports(ReportStatus.PENDING, from, to, pageable);

            assertThat(result.getContent()).hasSize(2);
            ReportGroupResponseDTO postGroup = result.getContent().get(0);
            assertThat(postGroup.targetType).isEqualTo(TargetType.POST);
            assertThat(postGroup.targetId).isEqualTo(10L);
            assertThat(postGroup.targetNickname).isEqualTo("post-writer");
            assertThat(postGroup.targetTitle).isEqualTo("reported post");
            assertThat(postGroup.targetContent).isEqualTo("post content");
            assertThat(postGroup.reportCount).isEqualTo(2L);
            assertThat(postGroup.reasonTypes).containsExactly("SPAM");

            ReportGroupResponseDTO commentGroup = result.getContent().get(1);
            assertThat(commentGroup.targetType).isEqualTo(TargetType.COMMENT);
            assertThat(commentGroup.targetId).isEqualTo(20L);
            assertThat(commentGroup.targetNickname).isEqualTo("comment-writer");
            assertThat(commentGroup.targetTitle).isNull();
            assertThat(commentGroup.targetContent).isEqualTo("comment content");
            assertThat(commentGroup.reportCount).isEqualTo(3L);
            assertThat(commentGroup.reasonTypes).containsExactly("ABUSE", "HATE");
        }

        @Test
        @DisplayName("rejects null or inverted date range")
        void getGroupedReports_rejectsInvalidDateRange() {
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 1, 1, 0, 0);

            assertServiceError(
                    () -> adminReportService.getGroupedReports(null, from, to, pageable),
                    ErrorCode.BAD_REQUEST
            );
        }

        @Test
        @DisplayName("rejects date ranges longer than ninety days")
        void getGroupedReports_rejectsTooWideDateRange() {
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 4, 2, 0, 0);

            assertServiceError(
                    () -> adminReportService.getGroupedReports(null, from, to, pageable),
                    ErrorCode.BAD_REQUEST
            );
        }

        @Test
        @DisplayName("rejects page size over one hundred")
        void getGroupedReports_rejectsTooLargePageSize() {
            Pageable pageable = PageRequest.of(0, 101);
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 1, 2, 0, 0);

            assertServiceError(
                    () -> adminReportService.getGroupedReports(null, from, to, pageable),
                    ErrorCode.BAD_REQUEST
            );
        }

        @Test
        @DisplayName("rejects unsupported sort")
        void getGroupedReports_rejectsUnsupportedSort() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 1, 2, 0, 0);

            assertServiceError(
                    () -> adminReportService.getGroupedReports(null, from, to, pageable),
                    ErrorCode.BAD_REQUEST
            );
        }
    }

    @Nested
    @DisplayName("approveReport")
    class ApproveReport {

        @Test
        @DisplayName("processes a pending report and delegates target handling")
        void approveReport_success() {
            Report report = report(ReportStatus.PENDING);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));
            given(reportTargetHandler.exists(TargetType.POST, 10L)).willReturn(true);
            AdminReportRequestDTO dto = new AdminReportRequestDTO(100L, TargetType.POST, "note", SanctionType.WARNED, null);

            adminReportService.approveReport(1L, dto);

            verify(report).processReport(admin);
            verify(reportTargetHandler).handleApproved(TargetType.POST, 10L, admin, SanctionType.WARNED, null);
        }

        @Test
        @DisplayName("throws when admin member does not exist")
        void approveReport_throwsWhenAdminMissing() {
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            assertServiceError(
                    () -> adminReportService.approveReport(1L, approveDto()),
                    MemberErrorCode.MEMBER_NOT_FOUND
            );
            verifyNoInteractions(reportRepository, reportTargetHandler);
        }

        @Test
        @DisplayName("throws when member is not an admin")
        void approveReport_throwsWhenNotAdmin() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(user));

            assertServiceError(
                    () -> adminReportService.approveReport(1L, approveDto()),
                    ReportErrorCode.REPORT_403_UNAUTHORIZED_ADMIN
            );
            verifyNoInteractions(reportRepository, reportTargetHandler);
        }

        @Test
        @DisplayName("throws when report does not exist")
        void approveReport_throwsWhenReportMissing() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.empty());

            assertServiceError(
                    () -> adminReportService.approveReport(1L, approveDto()),
                    ReportErrorCode.REPORT_404_REPORT
            );
        }

        @Test
        @DisplayName("throws when report is already processed")
        void approveReport_throwsWhenAlreadyProcessed() {
            Report report = report(ReportStatus.RESOLVED);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            assertServiceError(
                    () -> adminReportService.approveReport(1L, approveDto()),
                    ReportErrorCode.REPORT_409_ALREADY_REPORT
            );
            verify(reportTargetHandler, never()).handleApproved(TargetType.POST, 10L, admin, SanctionType.WARNED, null);
        }

        @Test
        @DisplayName("throws when target does not exist")
        void approveReport_throwsWhenTargetMissing() {
            Report report = report(ReportStatus.PENDING);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));
            given(reportTargetHandler.exists(TargetType.POST, 10L)).willReturn(false);

            assertServiceError(
                    () -> adminReportService.approveReport(1L, approveDto()),
                    ReportErrorCode.REPORT_404_TARGET
            );
        }

        @Test
        @DisplayName("throws when suspension has no positive day count")
        void approveReport_throwsWhenSuspensionDaysInvalid() {
            Report report = report(ReportStatus.PENDING);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));
            given(reportTargetHandler.exists(TargetType.POST, 10L)).willReturn(true);
            AdminReportRequestDTO dto = new AdminReportRequestDTO(100L, TargetType.POST, "note", SanctionType.SUSPENDED, 0);

            assertServiceError(
                    () -> adminReportService.approveReport(1L, dto),
                    ReportErrorCode.REPORT_400_INVALID_SANCTION_PARAMETER
            );
            verify(report, never()).processReport(admin);
            verify(reportTargetHandler, never()).handleApproved(TargetType.POST, 10L, admin, SanctionType.SUSPENDED, 0);
        }
    }

    @Nested
    @DisplayName("rejectReport")
    class RejectReport {

        @Test
        @DisplayName("rejects a pending report and delegates target handling")
        void rejectReport_success() {
            Report report = report(ReportStatus.PENDING);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            adminReportService.rejectReport(1L, approveDto());

            verify(report).rejectReport(admin);
            verify(reportTargetHandler).handleRejected(TargetType.POST, 10L, admin);
        }

        @Test
        @DisplayName("throws when report is already processed")
        void rejectReport_throwsWhenAlreadyProcessed() {
            Report report = report(ReportStatus.REJECTED);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(100L)).willReturn(Optional.of(report));

            assertServiceError(
                    () -> adminReportService.rejectReport(1L, approveDto()),
                    ReportErrorCode.REPORT_409_ALREADY_REPORT
            );
            verify(reportTargetHandler, never()).handleRejected(TargetType.POST, 10L, admin);
        }
    }

    @Nested
    @DisplayName("approveReportGroup")
    class ApproveReportGroup {

        @Test
        @DisplayName("bulk resolves pending reports and delegates target handling")
        void approveReportGroup_success() {
            AdminReportRequestDTO dto = new AdminReportRequestDTO(10L, TargetType.POST, "note", SanctionType.WARNED, null);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportTargetHandler.exists(TargetType.POST, 10L)).willReturn(true);
            given(reportRepository.updateStatusGroup(TargetType.POST, 10L, admin, ReportStatus.RESOLVED, ReportStatus.PENDING))
                    .willReturn(2);

            adminReportService.approveReportGroup(1L, dto);

            verify(reportRepository).updateStatusGroup(TargetType.POST, 10L, admin, ReportStatus.RESOLVED, ReportStatus.PENDING);
            verify(reportTargetHandler).handleApproved(TargetType.POST, 10L, admin, SanctionType.WARNED, null);
        }

        @Test
        @DisplayName("throws when there are no pending reports to approve")
        void approveReportGroup_throwsWhenNoPendingReports() {
            AdminReportRequestDTO dto = new AdminReportRequestDTO(10L, TargetType.POST, "note", SanctionType.WARNED, null);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportTargetHandler.exists(TargetType.POST, 10L)).willReturn(true);
            given(reportRepository.updateStatusGroup(TargetType.POST, 10L, admin, ReportStatus.RESOLVED, ReportStatus.PENDING))
                    .willReturn(0);

            assertServiceError(
                    () -> adminReportService.approveReportGroup(1L, dto),
                    ReportErrorCode.REPORT_404_PENDING_LIST
            );
            verify(reportTargetHandler, never()).handleApproved(TargetType.POST, 10L, admin, SanctionType.WARNED, null);
        }
    }

    @Nested
    @DisplayName("rejectReportGroup")
    class RejectReportGroup {

        @Test
        @DisplayName("bulk rejects pending reports and delegates target handling")
        void rejectReportGroup_success() {
            AdminReportRequestDTO dto = new AdminReportRequestDTO(10L, TargetType.POST, "note", null, null);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.updateStatusGroup(TargetType.POST, 10L, admin, ReportStatus.REJECTED, ReportStatus.PENDING))
                    .willReturn(1);

            adminReportService.rejectReportGroup(1L, dto);

            verify(reportRepository).updateStatusGroup(TargetType.POST, 10L, admin, ReportStatus.REJECTED, ReportStatus.PENDING);
            verify(reportTargetHandler).handleRejected(TargetType.POST, 10L, admin);
        }

        @Test
        @DisplayName("throws when there are no pending reports to reject")
        void rejectReportGroup_throwsWhenNoPendingReports() {
            AdminReportRequestDTO dto = new AdminReportRequestDTO(10L, TargetType.POST, "note", null, null);
            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.updateStatusGroup(TargetType.POST, 10L, admin, ReportStatus.REJECTED, ReportStatus.PENDING))
                    .willReturn(0);

            assertServiceError(
                    () -> adminReportService.rejectReportGroup(1L, dto),
                    ReportErrorCode.REPORT_404_PENDING_LIST
            );
            verify(reportTargetHandler, never()).handleRejected(TargetType.POST, 10L, admin);
        }
    }

    @Nested
    @DisplayName("approveReportGroupById")
    class ApproveReportGroupById {

        @Test
        @DisplayName("approves report group and resolves pending reports during transition")
        void approveReportGroupById_success() {
            ReportGroup reportGroup = reportGroup();
            ApproveReportGroupRequest request =
                    new ApproveReportGroupRequest("note", SanctionType.WARNED, null);

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportGroupRepository.findById(10L)).willReturn(Optional.of(reportGroup));
            given(reportTargetHandler.exists(TargetType.POST, 100L)).willReturn(true);
            given(reportRepository.updateStatusByReportGroupId(10L, admin, ReportStatus.RESOLVED, ReportStatus.PENDING))
                    .willReturn(2);

            adminReportService.approveReportGroupById(1L, 10L, request);

            assertThat(reportGroup.getStatus()).isEqualTo(ReportGroupStatus.APPROVED);
            verify(reportGroupRepository).saveAndFlush(reportGroup);
            verify(reportRepository).updateStatusByReportGroupId(10L, admin, ReportStatus.RESOLVED, ReportStatus.PENDING);
            verify(reportTargetHandler).handleApproved(TargetType.POST, 100L, admin, SanctionType.WARNED, null);
        }

        @Test
        @DisplayName("throws when report group does not exist")
        void approveReportGroupById_throwsWhenGroupMissing() {
            ApproveReportGroupRequest request =
                    new ApproveReportGroupRequest("note", SanctionType.WARNED, null);

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportGroupRepository.findById(10L)).willReturn(Optional.empty());

            assertServiceError(
                    () -> adminReportService.approveReportGroupById(1L, 10L, request),
                    ReportErrorCode.REPORT_404_REPORT_GROUP
            );
            verify(reportRepository, never()).updateStatusByReportGroupId(anyLong(), any(), any(), any());
            verifyNoInteractions(reportTargetHandler);
        }
    }

    @Nested
    @DisplayName("rejectReportGroupById")
    class RejectReportGroupById {

        @Test
        @DisplayName("rejects report group and rejects pending reports during transition")
        void rejectReportGroupById_success() {
            ReportGroup reportGroup = reportGroup();
            RejectReportGroupRequest request =
                    new RejectReportGroupRequest("not enough evidence");

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportGroupRepository.findById(10L)).willReturn(Optional.of(reportGroup));
            given(reportRepository.updateStatusByReportGroupId(10L, admin, ReportStatus.REJECTED, ReportStatus.PENDING))
                    .willReturn(2);

            adminReportService.rejectReportGroupById(1L, 10L, request);

            assertThat(reportGroup.getStatus()).isEqualTo(ReportGroupStatus.REJECTED);
            verify(reportGroupRepository).saveAndFlush(reportGroup);
            verify(reportRepository).updateStatusByReportGroupId(10L, admin, ReportStatus.REJECTED, ReportStatus.PENDING);
            verify(reportTargetHandler).handleRejected(TargetType.POST, 100L, admin);
        }
    }

    private ReportGroup reportGroup() {
        return new ReportGroup(
                TargetType.POST,
                100L,
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }

    private ReportGroup reportGroup(
            Long reportGroupId,
            TargetType targetType,
            Long targetId,
            int reportCount,
            LocalDateTime latestReportedAt
    ) {
        ReportGroup reportGroup =
                new ReportGroup(targetType, targetId, latestReportedAt);

        for (int i = 0; i < reportCount; i++) {
            reportGroup.registerReport(latestReportedAt);
        }

        setField(reportGroup, "reportGroupId", reportGroupId);
        return reportGroup;
    }

    private ReportReasonStatProjection reasonStat(
            Long reportGroupId,
            String reasonType,
            Long reasonCount
    ) {
        return new ReportReasonStatProjection() {
            @Override
            public long getReportGroupId() {
                return reportGroupId;
            }

            @Override
            public String getReasonType() {
                return reasonType;
            }

            @Override
            public long getReasonCount() {
                return reasonCount;
            }
        };
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Report report(ReportStatus status) {
        Report report = mock(Report.class);
        given(report.getStatus()).willReturn(status);
        given(report.getTargetType()).willReturn(TargetType.POST);
        given(report.getTargetId()).willReturn(10L);
        return report;
    }

    private AdminReportRequestDTO approveDto() {
        return new AdminReportRequestDTO(100L, TargetType.POST, "note", SanctionType.WARNED, null);
    }

    private void assertServiceError(Runnable action, ErrorCodeSpec expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(expectedErrorCode);
    }
}
