package com.back.devc.domain.interaction.report.unit

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
import com.back.devc.global.exception.ErrorCodeSpec
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.ReportSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.util.List

@DisplayName("AdminReportController")
internal class AdminReportControllerTest {
    private val adminReportService: AdminReportService =
        Mockito.mock<AdminReportService>(AdminReportService::class.java)

    private val controller = AdminReportController(adminReportService)

    private val adminPrincipal = JwtPrincipal(1L, "admin@test.com", "ADMIN")

    @Test
    @DisplayName("getReports returns raw report page")
    fun getReports_returnsRawReports() {
        val pageable = PageRequest.of(0, 10)

        val report = Mockito.mock<ReportResponseDTO>(ReportResponseDTO::class.java)

        val page = PageImpl<ReportResponseDTO?>(List.of<ReportResponseDTO?>(report), pageable, 1)

        Mockito.`when`<Page<ReportResponseDTO>>(adminReportService.getReports(ReportStatus.PENDING, pageable))
            .thenReturn(page)

        val response: ResponseEntity<SuccessResponse<Page<ReportResponseDTO?>?>?> = controller.getReports(
            ReportStatus.PENDING,
            pageable
        )

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode())
            .isEqualTo(HttpStatus.OK)

        Assertions.assertThat<SuccessResponse<Page<ReportResponseDTO>?>>(response.getBody())
            .isNotNull()

        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_200_LIST.code)

        Assertions.assertThat<ReportResponseDTO>(response.getBody()!!.data)
            .isSameAs(page)

        Mockito.verify<AdminReportService?>(adminReportService)
            .getReports(ReportStatus.PENDING, pageable)
    }

    @Test
    @DisplayName("getGrouped uses default service method when date range is absent")
    fun getGrouped_usesDefaultRange() {
        val pageable = PageRequest.of(0, 10)

        val group = Mockito.mock<ReportGroupResponseDTO>(ReportGroupResponseDTO::class.java)

        val page = PageImpl<ReportGroupResponseDTO?>(List.of<ReportGroupResponseDTO?>(group), pageable, 1)

        Mockito.`when`<Page<ReportGroupResponseDTO>>(
            adminReportService.getGroupedReports(
                ReportStatus.PENDING,
                pageable
            )
        ).thenReturn(page)

        val response: ResponseEntity<SuccessResponse<Page<ReportGroupResponseDTO?>?>?> = controller.getGrouped(
            adminPrincipal,
            ReportStatus.PENDING,
            null,
            null,
            pageable
        )

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode())
            .isEqualTo(HttpStatus.OK)

        Assertions.assertThat<SuccessResponse<Page<ReportGroupResponseDTO>?>>(response.getBody())
            .isNotNull()

        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_LIST.code)

        Assertions.assertThat<ReportGroupResponseDTO>(response.getBody()!!.data)
            .isSameAs(page)

        Mockito.verify<AdminReportService?>(adminReportService)
            .getGroupedReports(ReportStatus.PENDING, pageable)
    }

    @Test
    @DisplayName("getGrouped uses provided date range")
    fun getGrouped_usesProvidedRange() {
        val pageable = PageRequest.of(0, 10)

        val from =
            LocalDateTime.of(2026, 1, 1, 0, 0)

        val to =
            LocalDateTime.of(2026, 1, 2, 0, 0)

        val page =
            PageImpl<ReportGroupResponseDTO?>(
                mutableListOf<ReportGroupResponseDTO?>(),
                pageable,
                0
            )

        Mockito.`when`<Page<ReportGroupResponseDTO>>(
            adminReportService.getGroupedReports(
                null,
                from,
                to,
                pageable
            )
        ).thenReturn(page)

        val response: ResponseEntity<SuccessResponse<Page<ReportGroupResponseDTO?>?>?> = controller.getGrouped(
            adminPrincipal,
            null,
            from,
            to,
            pageable
        )

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode())
            .isEqualTo(HttpStatus.OK)

        Assertions.assertThat<SuccessResponse<Page<ReportGroupResponseDTO>?>>(response.getBody())
            .isNotNull()

        Assertions.assertThat<ReportGroupResponseDTO>(response.getBody()!!.data)
            .isSameAs(page)

        Mockito.verify<AdminReportService?>(adminReportService)
            .getGroupedReports(null, from, to, pageable)
    }

    @Test
    @DisplayName("approveGroup delegates to service and returns ok response")
    fun approveGroup_returnsOkResponse() {
        val dto =
            AdminReportRequestDTO(
                10L,
                TargetType.POST,
                "note",
                SanctionType.WARNED,
                null
            )

        val response: ResponseEntity<SuccessResponse<Void?>?> = controller.approveGroup(dto, adminPrincipal)

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode())
            .isEqualTo(HttpStatus.OK)

        Assertions.assertThat<SuccessResponse<Void?>>(response.getBody())
            .isNotNull()

        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.code)

        Assertions.assertThat(response.getBody()!!.message)
            .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_APPROVE.message)

        Assertions.assertThat<Void>(response.getBody()!!.data)
            .isNull()

        Mockito.verify<AdminReportService?>(adminReportService)
            .approveReportGroup(1L, dto)
    }

    @Test
    @DisplayName("rejectGroup delegates to service and returns ok response")
    fun rejectGroup_returnsOkResponse() {
        val dto =
            AdminReportRequestDTO(
                10L,
                TargetType.POST,
                "note",
                null,
                null
            )

        val response: ResponseEntity<SuccessResponse<Void?>?> = controller.rejectGroup(dto, adminPrincipal)

        Assertions.assertThat<HttpStatusCode?>(response.getStatusCode())
            .isEqualTo(HttpStatus.OK)

        Assertions.assertThat<SuccessResponse<Void?>>(response.getBody())
            .isNotNull()

        Assertions.assertThat(response.getBody()!!.code)
            .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.code)

        Assertions.assertThat(response.getBody()!!.message)
            .isEqualTo(ReportSuccessCode.REPORT_200_GROUP_REJECT.message)

        Assertions.assertThat<Void>(response.getBody()!!.data)
            .isNull()

        Mockito.verify<AdminReportService?>(adminReportService)
            .rejectReportGroup(1L, dto)
    }

    @Test
    @DisplayName("protected group endpoints throw unauthorized when principal is missing")
    fun protectedGroupEndpoints_throwUnauthorizedWithoutPrincipal() {
        val dto =
            AdminReportRequestDTO(
                10L,
                TargetType.POST,
                "note",
                null,
                null
            )

        assertUnauthorized(Runnable {
            controller.getGrouped(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
            )
        }
        )

        assertUnauthorized(Runnable { controller.approveGroup(dto, null) }
        )

        assertUnauthorized(Runnable { controller.rejectGroup(dto, null) }
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
