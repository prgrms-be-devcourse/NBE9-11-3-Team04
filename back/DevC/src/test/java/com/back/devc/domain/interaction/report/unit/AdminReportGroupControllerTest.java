package com.back.devc.domain.interaction.report.unit;

import com.back.devc.domain.interaction.report.controller.AdminReportGroupController;
import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest;
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest;
import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.service.AdminReportService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.response.successCode.ReportSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("AdminReportGroupController")
class AdminReportGroupControllerTest {

    private final AdminReportService adminReportService = mock(AdminReportService.class);

    private final AdminReportGroupController controller =
            new AdminReportGroupController(adminReportService);

    private final JwtPrincipal adminPrincipal =
            new JwtPrincipal(1L, "admin@test.com", "ADMIN");

    @Test
    @DisplayName("approveReportGroup delegates by reportGroupId and returns ok response")
    void approveReportGroup_returnsOkResponse() {
        ApproveReportGroupRequest request =
                new ApproveReportGroupRequest("note", SanctionType.WARNED, null);

        var response = controller.approveReportGroup(10L, request, adminPrincipal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.getCode());
        assertThat(response.getBody().getData()).isNull();
        verify(adminReportService).approveReportGroupById(1L, 10L, request);
    }

    @Test
    @DisplayName("rejectReportGroup delegates by reportGroupId and returns ok response")
    void rejectReportGroup_returnsOkResponse() {
        RejectReportGroupRequest request =
                new RejectReportGroupRequest("not enough evidence");

        var response = controller.rejectReportGroup(10L, request, adminPrincipal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.getCode());
        assertThat(response.getBody().getData()).isNull();
        verify(adminReportService).rejectReportGroupById(1L, 10L, request);
    }

    @Test
    @DisplayName("throws unauthorized when principal is missing")
    void throwsUnauthorizedWithoutPrincipal() {
        ApproveReportGroupRequest approveRequest =
                new ApproveReportGroupRequest("note", SanctionType.WARNED, null);
        RejectReportGroupRequest rejectRequest =
                new RejectReportGroupRequest("not enough evidence");

        assertUnauthorized(() ->
                controller.approveReportGroup(10L, approveRequest, null)
        );
        assertUnauthorized(() ->
                controller.rejectReportGroup(10L, rejectRequest, null)
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
