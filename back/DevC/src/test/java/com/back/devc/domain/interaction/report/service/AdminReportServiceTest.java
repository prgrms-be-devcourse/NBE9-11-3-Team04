package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO;
import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.interaction.report.util.ReportTargetHandler;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminReportServiceTest {

    @InjectMocks
    private AdminReportService adminReportService;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ReportTargetHandler reportTargetHandler;

    private Member admin;
    private Member user;

    @BeforeEach
    void setUp() {
        admin = mock(Member.class);
        user = mock(Member.class);

        when(admin.isAdmin()).thenReturn(true);
        when(admin.getUserId()).thenReturn(1L);

        when(user.isAdmin()).thenReturn(false);
    }

    // =========================================================
    // helper
    // =========================================================
    private Report report(ReportStatus status) {
        Report r = mock(Report.class);
        when(r.getStatus()).thenReturn(status);
        when(r.getTargetType()).thenReturn(TargetType.POST);
        when(r.getTargetId()).thenReturn(10L);
        return r;
    }

    private AdminReportRequestDTO dto() {
        return new AdminReportRequestDTO(
                1L,
                TargetType.POST,
                "note",
                SanctionType.WARNED,
                null
        );
    }



//     AdminReportServiceTest→ “흐름 + 검증 + 위임” 테스트
//     검증 대상:
//     admin 권한 체크
//     report 존재 여부
//     status 검증
//     target 존재 여부 (handler.exists)
//     handler 호출 여부
//     group 처리 흐름
//

    // =========================================================
    // 1. 목록 조회
    // =========================================================
    @Nested
    class GetReportsTest {

        @Test
        void status_null이면_findAll() {

            Pageable pageable = PageRequest.of(0, 10);
            Report r = report(ReportStatus.PENDING);

            given(reportRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(r)));

            given(reportTargetHandler.toDtoWithTargetInfo(r))
                    .willReturn(mock());

            var result = adminReportService.getReports(null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(reportRepository).findAll(pageable);
        }

        @Test
        void status_있으면_findAllByStatus() {

            Pageable pageable = PageRequest.of(0, 10);
            Report r = report(ReportStatus.PENDING);

            given(reportRepository.findAllByStatus(ReportStatus.PENDING, pageable))
                    .willReturn(new PageImpl<>(List.of(r)));

            given(reportTargetHandler.toDtoWithTargetInfo(r))
                    .willReturn(mock());

            var result = adminReportService.getReports(ReportStatus.PENDING, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // =========================================================
    // 2. 단건 승인
    // =========================================================
    @Nested
    class ApproveReportTest {

        @Test
        void 성공() {

            Report report = report(ReportStatus.PENDING);

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));

            given(reportTargetHandler.exists(TargetType.POST, 10L))
                    .willReturn(true);

            AdminReportRequestDTO dto = dto();

            adminReportService.approveReport(1L, dto);

            verify(report).processReport(admin);

            verify(reportTargetHandler).handleApproved(
                    TargetType.POST,
                    10L,
                    admin,
                    SanctionType.WARNED,
                    null
            );
        }

        @Test
        void 관리자가_아니면_예외() {

            Report report = report(ReportStatus.PENDING);

            given(memberRepository.findById(1L)).willReturn(Optional.of(user));
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));

            assertThatThrownBy(() ->
                    adminReportService.approveReport(1L, dto())
            ).isInstanceOf(ApiException.class);

            verify(report, never()).processReport(any());
        }

        @Test
        void 신고없으면_예외() {

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    adminReportService.approveReport(1L, dto())
            ).isInstanceOf(ApiException.class);
        }

        @Test
        void target없으면_예외() {

            Report report = report(ReportStatus.PENDING);

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));

            given(reportTargetHandler.exists(TargetType.POST, 10L))
                    .willReturn(false);

            assertThatThrownBy(() ->
                    adminReportService.approveReport(1L, dto())
            ).isInstanceOf(ApiException.class);
        }
    }

    // =========================================================
    // 3. 단건 반려
    // =========================================================
    @Nested
    class RejectReportTest {

        @Test
        void 성공() {

            Report report = report(ReportStatus.PENDING);

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));
            given(reportRepository.findById(1L)).willReturn(Optional.of(report));

            adminReportService.rejectReport(1L, dto());

            verify(report).rejectReport(admin);

            verify(reportTargetHandler).handleRejected(
                    TargetType.POST,
                    10L,
                    admin
            );
        }
    }

    // =========================================================
    // 4. 그룹 승인
    // =========================================================
    @Nested
    class ApproveGroupTest {

        @Test
        void 성공() {
            // 1. Given: 신고 데이터 준비
            Report r1 = report(ReportStatus.PENDING);
            Report r2 = report(ReportStatus.PENDING);

            given(memberRepository.findById(1L))
                    .willReturn(Optional.of(admin));

            given(reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                    eq(TargetType.POST), anyLong(), eq(ReportStatus.PENDING)
            )).willReturn(List.of(r1, r2));

            lenient().when(postRepository.existsById(anyLong())).thenReturn(true);
            lenient().when(reportTargetHandler.exists(any(), anyLong())).thenReturn(true);

            AdminReportRequestDTO dto = new AdminReportRequestDTO(
                    1L,
                    TargetType.POST,
                    "10",
                    SanctionType.WARNED,
                    null
            );

            // 2. When: 실행
            adminReportService.approveReportGroup(1L, dto);

            // 3. Then: 검증
            verify(r1).processReport(admin);
            verify(r2).processReport(admin);
        }

        @Test
        void 비어있으면_예외() {

            given(memberRepository.findById(1L)).willReturn(Optional.of(admin));

            given(reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                    TargetType.POST, 10L, ReportStatus.PENDING
            )).willReturn(List.of());

            assertThatThrownBy(() ->
                    adminReportService.approveReportGroup(1L, dto())
            ).isInstanceOf(ApiException.class);
        }
    }

    // =========================================================
    // 5. 그룹 반려
    // =========================================================
    @Nested
    class RejectGroupTest {

        @Test
        void 성공() {
            // 1. Given
            Report r1 = mock(Report.class);
            lenient().when(r1.getStatus()).thenReturn(ReportStatus.PENDING);

            given(memberRepository.findById(1L))
                    .willReturn(Optional.of(admin));

            given(reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                    eq(TargetType.POST), anyLong(), eq(ReportStatus.PENDING)
            )).willReturn(List.of(r1));

            AdminReportRequestDTO dto = new AdminReportRequestDTO(
                    1L,
                    TargetType.POST,
                    "10",
                    null,
                    null
            );

            // 2. When
            adminReportService.rejectReportGroup(1L, dto);

            // 3. Then
            verify(r1).rejectReport(admin);
        }
    }
}