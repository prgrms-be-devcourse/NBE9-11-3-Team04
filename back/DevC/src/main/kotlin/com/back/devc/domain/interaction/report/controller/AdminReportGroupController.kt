package com.back.devc.domain.interaction.report.controller

import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest
import com.back.devc.domain.interaction.report.service.AdminReportService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.ReportSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "관리자 신고 그룹 API", description = "관리자용 신고 그룹 승인/반려 API")
class AdminReportGroupController(
    private val adminReportService: AdminReportService
) {

    @PostMapping("/{reportGroupId}/approve")
    @Operation(summary = "신고 그룹 승인", description = "신고 그룹을 승인하고 대상 삭제, 알림, 선택 제재를 처리합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "신고 그룹 승인 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 제재 파라미터"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "404", description = "신고 그룹 또는 신고 대상 없음"),
            ApiResponse(responseCode = "409", description = "이미 처리된 신고 그룹")
        ]
    )
    fun approveReportGroup(
        @Parameter(description = "신고 그룹 ID", example = "1")
        @PathVariable
        reportGroupId: Long,

        @RequestBody
        @Valid
        request: ApproveReportGroupRequest,

        @Parameter(hidden = true)
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
    @Operation(summary = "신고 그룹 반려", description = "신고 그룹을 반려하고 신고자에게 처리 알림을 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "신고 그룹 반려 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "404", description = "신고 그룹 없음"),
            ApiResponse(responseCode = "409", description = "이미 처리된 신고 그룹")
        ]
    )
    fun rejectReportGroup(
        @Parameter(description = "신고 그룹 ID", example = "1")
        @PathVariable
        reportGroupId: Long,

        @RequestBody
        @Valid
        request: RejectReportGroupRequest,

        @Parameter(hidden = true)
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
