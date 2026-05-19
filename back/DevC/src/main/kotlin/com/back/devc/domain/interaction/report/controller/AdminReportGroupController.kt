package com.back.devc.domain.interaction.report.controller

import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest
import com.back.devc.domain.interaction.report.service.AdminReportService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.ReportSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/report-groups")
@PreAuthorize("hasRole('ADMIN')")
class AdminReportGroupController(
    private val adminReportService: AdminReportService
) {

    @PostMapping("/{reportGroupId}/approve")
    fun approveReportGroup(
        @PathVariable
        reportGroupId: Long,

        @RequestBody
        @Valid
        request: ApproveReportGroupRequest,

        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        adminReportService.approveReportGroupById(
            getAuthenticatedUserId(principal),
            reportGroupId,
            request
        )

        return ResponseEntity
            .status(ReportSuccessCode.REPORT_200_GROUP_APPROVE.status)
            .body(
                SuccessResponse.of(
                    ReportSuccessCode.REPORT_200_GROUP_APPROVE,
                    null
                )
            )
    }

    @PostMapping("/{reportGroupId}/reject")
    fun rejectReportGroup(
        @PathVariable
        reportGroupId: Long,

        @RequestBody
        @Valid
        request: RejectReportGroupRequest,

        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        adminReportService.rejectReportGroupById(
            getAuthenticatedUserId(principal),
            reportGroupId,
            request
        )

        return ResponseEntity
            .status(ReportSuccessCode.REPORT_200_GROUP_REJECT.status)
            .body(
                SuccessResponse.of(
                    ReportSuccessCode.REPORT_200_GROUP_REJECT,
                    null
                )
            )
    }

    private fun getAuthenticatedUserId(
        principal: JwtPrincipal?
    ): Long {

        return principal?.userId
            ?: throw ApiException(ErrorCode.UNAUTHORIZED)
    }
}
