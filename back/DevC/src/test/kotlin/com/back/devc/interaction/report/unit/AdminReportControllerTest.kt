package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.controller.AdminReportController
import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
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
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")

@DisplayName("AdminReportController")
internal class AdminReportControllerTest {
    private val adminReportService = mock<AdminReportService>()
    private val controller = AdminReportController(adminReportService)
    private val adminPrincipal = JwtPrincipal(1L, "admin@test.com", "ADMIN")

    @Test
    @DisplayName("getReports returns raw report page")
    fun getReports_returnsRawReports() {
        val pageable = PageRequest.of(0, 10)
        val report = mock<ReportResponseDTO>()
        val page = PageImpl(listOf(report), pageable, 1)

        whenever(adminReportService.getReports(ReportStatus.PENDING, pageable))
            .thenReturn(page)

        val response = controller.getReports(ReportStatus.PENDING, pageable)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_200_LIST.code)
        assertThat(body.data).isSameAs(page)
        verify(adminReportService).getReports(ReportStatus.PENDING, pageable)
    }

    @Test
    @DisplayName("getGrouped uses default service method when date range is absent")
    fun getGrouped_usesDefaultRange() {
        val pageable = PageRequest.of(0, 10)
        val group = mock<ReportGroupResponseDTO>()
        val page = PageImpl(listOf(group), pageable, 1)

        whenever(adminReportService.getGroupedReports(ReportStatus.PENDING, pageable))
            .thenReturn(page)

        val response = controller.getGrouped(adminPrincipal, ReportStatus.PENDING, null, null, pageable)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_200_GROUP_LIST.code)
        assertThat(body.data).isSameAs(page)
        verify(adminReportService).getGroupedReports(ReportStatus.PENDING, pageable)
    }

    @Test
    @DisplayName("getGrouped uses provided date range")
    fun getGrouped_usesProvidedRange() {
        val pageable = PageRequest.of(0, 10)
        val from = LocalDateTime.of(2026, 1, 1, 0, 0)
        val to = LocalDateTime.of(2026, 1, 2, 0, 0)
        val page = PageImpl(emptyList<ReportGroupResponseDTO>(), pageable, 0)

        whenever(adminReportService.getGroupedReports(null, from, to, pageable))
            .thenReturn(page)

        val response = controller.getGrouped(adminPrincipal, null, from, to, pageable)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(body.data).isSameAs(page)
        verify(adminReportService).getGroupedReports(null, from, to, pageable)
    }

    @Test
    @DisplayName("approveGroup delegates to service and returns ok response")
    fun approveGroup_returnsOkResponse() {
        val dto = legacyGroupRequest(sanctionType = SanctionType.WARNED)

        val response = controller.approveGroup(dto, adminPrincipal)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.code)
        assertThat(body.message).isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.message)
        assertThat(body.data).isNull()
        verify(adminReportService).approveReportGroup(1L, dto)
    }

    @Test
    @DisplayName("rejectGroup delegates to service and returns ok response")
    fun rejectGroup_returnsOkResponse() {
        val dto = legacyGroupRequest()

        val response = controller.rejectGroup(dto, adminPrincipal)
        val body = response.successBody()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(body.code).isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.code)
        assertThat(body.message).isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.message)
        assertThat(body.data).isNull()
        verify(adminReportService).rejectReportGroup(1L, dto)
    }

    @Test
    @DisplayName("protected group endpoints throw unauthorized when principal is missing")
    fun protectedGroupEndpoints_throwUnauthorizedWithoutPrincipal() {
        val dto = legacyGroupRequest()

        assertUnauthorized { controller.getGrouped(null, null, null, null, PageRequest.of(0, 10)) }
        assertUnauthorized { controller.approveGroup(dto, null) }
        assertUnauthorized { controller.rejectGroup(dto, null) }

        verifyNoInteractions(adminReportService)
    }

    private fun legacyGroupRequest(
        sanctionType: SanctionType? = null
    ): AdminReportRequestDTO =
        AdminReportRequestDTO(
            reportId = 10L,
            targetType = TargetType.POST,
            adminNote = "note",
            sanctionType = sanctionType,
            suspensionDays = null
        )

    private fun assertUnauthorized(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(ApiException::class.java)
            .extracting { error -> (error as ApiException).errorCode }
            .isEqualTo(ErrorCode.UNAUTHORIZED)
    }
}
