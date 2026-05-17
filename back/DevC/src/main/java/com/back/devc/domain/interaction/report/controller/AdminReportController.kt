package com.back.devc.domain.interaction.report.controller

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.service.AdminReportService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.ReportSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
class AdminReportController(
    private val adminReportService: AdminReportService
) {

    @GetMapping("/raw")
    fun getReports(
        @RequestParam(required = false)
        status: ReportStatus?,

        @PageableDefault(
            size = 20,
            sort = ["createdAt"],
            direction = Sort.Direction.DESC
        )
        pageable: Pageable
    ): ResponseEntity<SuccessResponse<Page<ReportResponseDTO>>> {

        val reports = adminReportService.getReports(status, pageable)

        val successCode = ReportSuccessCode.REPORT_200_LIST

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, reports))
    }

    @GetMapping("/groups")
    fun getGrouped(
        @AuthenticationPrincipal
        principal: JwtPrincipal?,

        @RequestParam(required = false)
        status: ReportStatus?,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,

        @PageableDefault(
            size = 20,
            sort = ["latestCreatedAt"],
            direction = Sort.Direction.DESC
        )
        pageable: Pageable
    ): ResponseEntity<SuccessResponse<Page<ReportGroupResponseDTO>>> {

        getAuthenticatedUserId(principal)

        val groups =
            if (from == null && to == null) {
                adminReportService.getGroupedReports(status, pageable)
            } else {
                adminReportService.getGroupedReports(status, from, to, pageable)
            }

        val successCode = ReportSuccessCode.REPORT_200_GROUP_LIST

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, groups))
    }

    @PostMapping("/groups/approve")
    fun approveGroup(
        @RequestBody @Valid
        requestDto: AdminReportRequestDTO,

        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        adminReportService.approveReportGroup(
            getAuthenticatedUserId(principal),
            requestDto
        )

        val successCode = ReportSuccessCode.REPORT_200_GROUP_APPROVE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, null))
    }

    @PostMapping("/groups/reject")
    fun rejectGroup(
        @RequestBody @Valid
        requestDto: AdminReportRequestDTO,

        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        adminReportService.rejectReportGroup(
            getAuthenticatedUserId(principal),
            requestDto
        )

        val successCode = ReportSuccessCode.REPORT_200_GROUP_REJECT

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, null))
    }

    private fun getAuthenticatedUserId(principal: JwtPrincipal?): Long {
        return principal?.userId()
            ?: throw ApiException(ErrorCode.UNAUTHORIZED)
    }
}
