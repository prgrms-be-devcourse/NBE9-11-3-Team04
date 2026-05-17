package com.back.devc.domain.interaction.report.controller;

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO;
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO;
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.service.AdminReportService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.response.successCode.ReportSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("AdminReportController")
class AdminReportControllerTest {

    private final AdminReportService adminReportService = mock(AdminReportService.class);

    private final AdminReportController controller =
            new AdminReportController(adminReportService);

    private final JwtPrincipal adminPrincipal =
            new JwtPrincipal(1L, "admin@test.com", "ADMIN");

    @Test
    @DisplayName("getReports returns raw report page")
    void getReports_returnsRawReports() {

        PageRequest pageable = PageRequest.of(0, 10);

        ReportResponseDTO report = mock(ReportResponseDTO.class);

        var page = new PageImpl<>(List.of(report), pageable, 1);

        when(adminReportService.getReports(ReportStatus.PENDING, pageable))
                .thenReturn(page);

        var response = controller.getReports(
                ReportStatus.PENDING,
                pageable
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().code())
                .isEqualTo(ReportSuccessCode.REPORT_200_LIST.getCode());

        assertThat(response.getBody().data())
                .isSameAs(page);

        verify(adminReportService)
                .getReports(ReportStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("getGrouped uses default service method when date range is absent")
    void getGrouped_usesDefaultRange() {

        PageRequest pageable = PageRequest.of(0, 10);

        ReportGroupResponseDTO group = mock(ReportGroupResponseDTO.class);

        var page = new PageImpl<>(List.of(group), pageable, 1);

        when(adminReportService.getGroupedReports(
                ReportStatus.PENDING,
                pageable
        )).thenReturn(page);

        var response = controller.getGrouped(
                adminPrincipal,
                ReportStatus.PENDING,
                null,
                null,
                pageable
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().code())
                .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_LIST.getCode());

        assertThat(response.getBody().data())
                .isSameAs(page);

        verify(adminReportService)
                .getGroupedReports(ReportStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("getGrouped uses provided date range")
    void getGrouped_usesProvidedRange() {

        PageRequest pageable = PageRequest.of(0, 10);

        LocalDateTime from =
                LocalDateTime.of(2026, 1, 1, 0, 0);

        LocalDateTime to =
                LocalDateTime.of(2026, 1, 2, 0, 0);

        var page =
                new PageImpl<ReportGroupResponseDTO>(
                        List.of(),
                        pageable,
                        0
                );

        when(adminReportService.getGroupedReports(
                null,
                from,
                to,
                pageable
        )).thenReturn(page);

        var response = controller.getGrouped(
                adminPrincipal,
                null,
                from,
                to,
                pageable
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().data())
                .isSameAs(page);

        verify(adminReportService)
                .getGroupedReports(null, from, to, pageable);
    }

    @Test
    @DisplayName("approveGroup delegates to service and returns ok response")
    void approveGroup_returnsOkResponse() {

        AdminReportRequestDTO dto =
                new AdminReportRequestDTO(
                        10L,
                        TargetType.POST,
                        "note",
                        SanctionType.WARNED,
                        null
                );

        var response = controller.approveGroup(dto, adminPrincipal);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().code())
                .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.getCode());

        assertThat(response.getBody().message())
                .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.getMessage());

        assertThat(response.getBody().data())
                .isNull();

        verify(adminReportService)
                .approveReportGroup(1L, dto);
    }

    @Test
    @DisplayName("rejectGroup delegates to service and returns ok response")
    void rejectGroup_returnsOkResponse() {

        AdminReportRequestDTO dto =
                new AdminReportRequestDTO(
                        10L,
                        TargetType.POST,
                        "note",
                        null,
                        null
                );

        var response = controller.rejectGroup(dto, adminPrincipal);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().code())
                .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.getCode());

        assertThat(response.getBody().message())
                .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.getMessage());

        assertThat(response.getBody().data())
                .isNull();

        verify(adminReportService)
                .rejectReportGroup(1L, dto);
    }

    @Test
    @DisplayName("protected group endpoints throw unauthorized when principal is missing")
    void protectedGroupEndpoints_throwUnauthorizedWithoutPrincipal() {

        AdminReportRequestDTO dto =
                new AdminReportRequestDTO(
                        10L,
                        TargetType.POST,
                        "note",
                        null,
                        null
                );

        assertUnauthorized(() ->
                controller.getGrouped(
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 10)
                )
        );

        assertUnauthorized(() ->
                controller.approveGroup(dto, null)
        );

        assertUnauthorized(() ->
                controller.rejectGroup(dto, null)
        );

        verifyNoInteractions(adminReportService);
    }

    private void assertUnauthorized(Runnable action) {

        assertThatThrownBy(action::run)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
