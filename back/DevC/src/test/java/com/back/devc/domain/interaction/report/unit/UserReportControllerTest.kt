package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.controller.UserReportController
import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.service.UserReportService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.exception.ErrorCodeSpec
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.ReportSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity

@DisplayName("UserReportController")
internal class UserReportControllerTest {
    private val reportService: UserReportService = Mockito.mock<UserReportService>(UserReportService::class.java)

    private val controller = UserReportController(reportService)

    @Test
    @DisplayName("reportPost delegates to service and returns created response")
    fun reportPost_returnsCreatedResponse() {
        val dto =
            ReportRequestDTO(
                10L,
                "SPAM",
                "Repeated promotion"
            )

        val principal =
            JwtPrincipal(
                1L,
                "user@test.com",
                "USER"
            )

        val response: ResponseEntity<SuccessResponse<Void?>?> = controller.reportPost(dto, principal)

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode())
            .isEqualTo(HttpStatus.CREATED)

        Assertions.assertThat<SuccessResponse<Void?>>(response.getBody())
            .isNotNull()

        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_201_POST.code)

        Assertions.assertThat(response.getBody()!!.message)
            .isEqualTo(ReportSuccessCode.REPORT_201_POST.message)

        Assertions.assertThat<Void>(response.getBody()!!.data)
            .isNull()

        Mockito.verify<UserReportService?>(reportService)
            .reportPost(1L, dto)
    }

    @Test
    @DisplayName("reportComment delegates to service and returns created response")
    fun reportComment_returnsCreatedResponse() {
        val dto =
            ReportRequestDTO(
                20L,
                "ABUSE",
                "Insulting content"
            )

        val principal =
            JwtPrincipal(
                1L,
                "user@test.com",
                "USER"
            )

        val response: ResponseEntity<SuccessResponse<Void?>?> = controller.reportComment(dto, principal)

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode())
            .isEqualTo(HttpStatus.CREATED)

        Assertions.assertThat<SuccessResponse<Void?>>(response.getBody())
            .isNotNull()

        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_201_COMMENT.code)

        Assertions.assertThat(response.getBody()!!.message)
            .isEqualTo(ReportSuccessCode.REPORT_201_COMMENT.message)

        Assertions.assertThat<Void>(response.getBody()!!.data)
            .isNull()

        Mockito.verify<UserReportService?>(reportService)
            .reportComment(1L, dto)
    }

    @Test
    @DisplayName("report endpoints throw unauthorized when principal is missing")
    fun reportEndpoints_throwUnauthorizedWithoutPrincipal() {
        val dto =
            ReportRequestDTO(
                10L,
                "SPAM",
                null
            )

        assertUnauthorized(Runnable { controller.reportPost(dto, null) }
        )

        assertUnauthorized(Runnable { controller.reportComment(dto, null) }
        )

        Mockito.verifyNoInteractions(reportService)
    }

    private fun assertUnauthorized(action: Runnable) {
        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable { action.run() })
            .isInstanceOf(ApiException::class.java)
            .extracting<ErrorCodeSpec> { e: Throwable? -> (e as ApiException).errorCode }
            .isEqualTo(ErrorCode.UNAUTHORIZED)
    }
}

