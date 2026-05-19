package com.back.devc.domain.interaction.report.controller;

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO;
import com.back.devc.domain.interaction.report.service.UserReportService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.response.successCode.ReportSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UserReportController")
class UserReportControllerTest {

    private final UserReportService reportService =
            mock(UserReportService.class);

    private final UserReportController controller =
            new UserReportController(reportService);

    @Test
    @DisplayName("reportPost delegates to service and returns created response")
    void reportPost_returnsCreatedResponse() {

        ReportRequestDTO dto =
                new ReportRequestDTO(
                        10L,
                        "SPAM",
                        "Repeated promotion"
                );

        JwtPrincipal principal =
                new JwtPrincipal(
                        1L,
                        "user@test.com",
                        "USER"
                );

        var response = controller.reportPost(dto, principal);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().getCode())
                .isEqualTo(ReportSuccessCode.REPORT_201_POST.getCode());

        assertThat(response.getBody().getMessage())
                .isEqualTo(ReportSuccessCode.REPORT_201_POST.getMessage());

        assertThat(response.getBody().getData())
                .isNull();

        verify(reportService)
                .reportPost(1L, dto);
    }

    @Test
    @DisplayName("reportComment delegates to service and returns created response")
    void reportComment_returnsCreatedResponse() {

        ReportRequestDTO dto =
                new ReportRequestDTO(
                        20L,
                        "ABUSE",
                        "Insulting content"
                );

        JwtPrincipal principal =
                new JwtPrincipal(
                        1L,
                        "user@test.com",
                        "USER"
                );

        var response = controller.reportComment(dto, principal);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
                .isNotNull();

        assertThat(response.getBody().getCode())
                .isEqualTo(ReportSuccessCode.REPORT_201_COMMENT.getCode());

        assertThat(response.getBody().getMessage())
                .isEqualTo(ReportSuccessCode.REPORT_201_COMMENT.getMessage());

        assertThat(response.getBody().getData())
                .isNull();

        verify(reportService)
                .reportComment(1L, dto);
    }

    @Test
    @DisplayName("report endpoints throw unauthorized when principal is missing")
    void reportEndpoints_throwUnauthorizedWithoutPrincipal() {

        ReportRequestDTO dto =
                new ReportRequestDTO(
                        10L,
                        "SPAM",
                        null
                );

        assertUnauthorized(() ->
                controller.reportPost(dto, null)
        );

        assertUnauthorized(() ->
                controller.reportComment(dto, null)
        );

        verifyNoInteractions(reportService);
    }

    private void assertUnauthorized(Runnable action) {

        assertThatThrownBy(action::run)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}

