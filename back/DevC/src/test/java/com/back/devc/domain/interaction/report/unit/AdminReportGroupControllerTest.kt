package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.controller.AdminReportGroupController
import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.service.AdminReportService
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

@DisplayName("AdminReportGroupController")
internal class AdminReportGroupControllerTest {
    private val adminReportService: AdminReportService =
        Mockito.mock<AdminReportService>(AdminReportService::class.java)

    private val controller = AdminReportGroupController(adminReportService)

    private val adminPrincipal = JwtPrincipal(1L, "admin@test.com", "ADMIN")

    @Test
    @DisplayName("approveReportGroup delegates by reportGroupId and returns ok response")
    fun approveReportGroup_returnsOkResponse() {
        val request =
            ApproveReportGroupRequest("note", SanctionType.WARNED, null)

        val response: ResponseEntity<SuccessResponse<Void?>?> =
            controller.approveReportGroup(10L, request, adminPrincipal)

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode()).isEqualTo(HttpStatus.OK)
        Assertions.assertThat<SuccessResponse<Void?>>(response.getBody()).isNotNull()
        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.code)
        Assertions.assertThat<Void>(response.getBody()!!.data).isNull()
        Mockito.verify<AdminReportService?>(adminReportService).approveReportGroupById(1L, 10L, request)
    }

    @Test
    @DisplayName("rejectReportGroup delegates by reportGroupId and returns ok response")
    fun rejectReportGroup_returnsOkResponse() {
        val request =
            RejectReportGroupRequest("not enough evidence")

        val response: ResponseEntity<SuccessResponse<Void?>?> =
            controller.rejectReportGroup(10L, request, adminPrincipal)

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode()).isEqualTo(HttpStatus.OK)
        Assertions.assertThat<SuccessResponse<Void?>>(response.getBody()).isNotNull()
        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.code)
        Assertions.assertThat<Void>(response.getBody()!!.data).isNull()
        Mockito.verify<AdminReportService?>(adminReportService).rejectReportGroupById(1L, 10L, request)
    }

    @Test
    @DisplayName("throws unauthorized when principal is missing")
    fun throwsUnauthorizedWithoutPrincipal() {
        val approveRequest =
            ApproveReportGroupRequest("note", SanctionType.WARNED, null)
        val rejectRequest =
            RejectReportGroupRequest("not enough evidence")

        assertUnauthorized(Runnable { controller.approveReportGroup(10L, approveRequest, null) }
        )
        assertUnauthorized(Runnable { controller.rejectReportGroup(10L, rejectRequest, null) }
        )

        Mockito.verifyNoInteractions(adminReportService)
    }

    private fun assertUnauthorized(action: Runnable) {
        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable { action.run() })
            .isInstanceOf(ApiException::class.java)
            .extracting<ErrorCodeSpec> { e: Throwable? -> (e as ApiException).errorCode }
            .isEqualTo(ErrorCode.UNAUTHORIZED)
    }
}
