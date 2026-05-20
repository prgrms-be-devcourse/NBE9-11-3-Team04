package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.controller.UserReportController
import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.service.UserReportService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.response.successCode.ReportSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.interaction.report.successBody
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserReportController")
internal class UserReportControllerTest {
    private val reportService = mock<UserReportService>()
    private val controller = UserReportController(reportService)
    private val principal = JwtPrincipal(1L, "user@test.com", "USER")

    @Test
    @DisplayName("reportPost delegates to service and returns created response")
    fun reportPost_returnsCreatedResponse() {
        val request = ReportRequestDTO(10L, "SPAM", "Repeated promotion")

        val response = controller.reportPost(request, principal)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_201_POST.code)
        assertThat(body.message).isEqualTo(ReportSuccessCode.REPORT_201_POST.message)
        assertThat(body.data).isNull()
        verify(reportService).reportPost(1L, request)
    }

    @Test
    @DisplayName("reportComment delegates to service and returns created response")
    fun reportComment_returnsCreatedResponse() {
        val request = ReportRequestDTO(20L, "ABUSE", "Insulting content")

        val response = controller.reportComment(request, principal)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_201_COMMENT.code)
        assertThat(body.message).isEqualTo(ReportSuccessCode.REPORT_201_COMMENT.message)
        assertThat(body.data).isNull()
        verify(reportService).reportComment(1L, request)
    }

    @Test
    @DisplayName("report endpoints throw unauthorized when principal is missing")
    fun reportEndpoints_throwUnauthorizedWithoutPrincipal() {
        val request = ReportRequestDTO(10L, "SPAM", null)

        assertUnauthorized { controller.reportPost(request, null) }
        assertUnauthorized { controller.reportComment(request, null) }

        verifyNoInteractions(reportService)
    }

    private fun assertUnauthorized(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(ApiException::class.java)
            .extracting { error -> (error as ApiException).errorCode }
            .isEqualTo(ErrorCode.UNAUTHORIZED)
    }
}
