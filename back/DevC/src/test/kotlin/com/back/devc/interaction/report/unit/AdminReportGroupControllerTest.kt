package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.controller.AdminReportGroupController
import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.service.AdminReportService
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

@DisplayName("AdminReportGroupController")
internal class AdminReportGroupControllerTest {
    private val adminReportService = mock<AdminReportService>()
    private val controller = AdminReportGroupController(adminReportService)
    private val adminPrincipal = JwtPrincipal(1L, "admin@test.com", "ADMIN")

    @Test
    @DisplayName("approveReportGroup delegates by reportGroupId and returns ok response")
    fun approveReportGroup_returnsOkResponse() {
        val request = ApproveReportGroupRequest("note", SanctionType.WARNED, null)

        val response = controller.approveReportGroup(10L, request, adminPrincipal)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.code)
        assertThat(body.data).isNull()
        verify(adminReportService).approveReportGroupById(1L, 10L, request)
    }

    @Test
    @DisplayName("rejectReportGroup delegates by reportGroupId and returns ok response")
    fun rejectReportGroup_returnsOkResponse() {
        val request = RejectReportGroupRequest("not enough evidence")

        val response = controller.rejectReportGroup(10L, request, adminPrincipal)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.code)
        assertThat(body.data).isNull()
        verify(adminReportService).rejectReportGroupById(1L, 10L, request)
    }

    @Test
    @DisplayName("throws unauthorized when principal is missing")
    fun throwsUnauthorizedWithoutPrincipal() {
        val approveRequest = ApproveReportGroupRequest("note", SanctionType.WARNED, null)
        val rejectRequest = RejectReportGroupRequest("not enough evidence")

        assertUnauthorized { controller.approveReportGroup(10L, approveRequest, null) }
        assertUnauthorized { controller.rejectReportGroup(10L, rejectRequest, null) }

        verifyNoInteractions(adminReportService)
    }

    private fun assertUnauthorized(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(ApiException::class.java)
            .extracting { error -> (error as ApiException).errorCode }
            .isEqualTo(ErrorCode.UNAUTHORIZED)
    }
}
