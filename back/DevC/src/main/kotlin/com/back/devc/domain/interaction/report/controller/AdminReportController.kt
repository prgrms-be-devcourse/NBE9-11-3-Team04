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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.slf4j.LoggerFactory
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
@Tag(name = "관리자 신고 API", description = "관리자용 신고 목록 조회 및 레거시 신고 그룹 처리 API")
class AdminReportController(
    private val adminReportService: AdminReportService
) {
    private val log = LoggerFactory.getLogger(AdminReportController::class.java)

    @GetMapping("/raw")
    @Operation(summary = "신고 단건 목록 조회", description = "관리자가 신고 단건 목록을 상태별로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "신고 단건 목록 조회 성공"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음")
        ]
    )
    fun getReports(
        @Parameter(description = "신고 처리 상태", example = "PENDING")
        @RequestParam(required = false)
        status: ReportStatus?,

        @ParameterObject
        @PageableDefault(
            size = 20,
            sort = ["createdAt"],
            direction = Sort.Direction.DESC
        )
        pageable: Pageable
    ): ResponseEntity<SuccessResponse<Page<ReportResponseDTO>>> {

        val reports = adminReportService.getReports(
            status,
            pageable
        )

        return ResponseEntity
            .status(ReportSuccessCode.REPORT_200_LIST.status)
            .body(
                SuccessResponse.of(
                    ReportSuccessCode.REPORT_200_LIST,
                    reports
                )
            )
    }

    @GetMapping("/groups")
    @Operation(summary = "신고 그룹 목록 조회", description = "관리자가 신고 그룹 목록을 상태와 기간 조건으로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "신고 그룹 목록 조회 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 기간 또는 정렬 조건"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음")
        ]
    )
    fun getGrouped(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        principal: JwtPrincipal?,

        @Parameter(description = "신고 처리 상태", example = "PENDING")
        @RequestParam(required = false)
        status: ReportStatus?,

        @Parameter(description = "조회 시작 시각", example = "2026-05-01T00:00:00")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,

        @Parameter(description = "조회 종료 시각", example = "2026-05-20T23:59:59")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,

        @ParameterObject
        @PageableDefault(
            size = 20,
            sort = ["latestCreatedAt"],
            direction = Sort.Direction.DESC
        )
        pageable: Pageable
    ): ResponseEntity<SuccessResponse<Page<ReportGroupResponseDTO>>> {

        getAuthenticatedUserId(principal)

        val groups = when {
            from == null && to == null -> {
                adminReportService.getGroupedReports(
                    status,
                    pageable
                )
            }

            from != null && to != null -> {
                adminReportService.getGroupedReports(
                    status,
                    from,
                    to,
                    pageable
                )
            }

            else -> {
                throw ApiException(ErrorCode.BAD_REQUEST)
            }
        }

        return ResponseEntity
            .status(ReportSuccessCode.REPORT_200_GROUP_LIST.status)
            .body(
                SuccessResponse.of(
                    ReportSuccessCode.REPORT_200_GROUP_LIST,
                    groups
                )
            )
    }

    @Deprecated(
        message = "Use POST /api/admin/report-groups/{reportGroupId}/approve instead."
    )
    @PostMapping("/groups/approve")
    @Operation(
        summary = "레거시 신고 그룹 승인",
        description = "targetType과 targetId 기반으로 신고 그룹을 승인합니다. 신규 API 사용을 권장합니다.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "레거시 신고 그룹 승인 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 제재 파라미터"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "404", description = "처리 가능한 신고 없음")
        ]
    )
    fun approveGroup(
        @RequestBody
        @Valid
        requestDto: AdminReportRequestDTO,

        @Parameter(hidden = true)
        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        val adminId = getAuthenticatedUserId(principal)

        log.warn(
            "Legacy report group approve API called - adminId={}, targetType={}, targetId={}",
            adminId,
            requestDto.targetType,
            requestDto.reportId
        )

        adminReportService.approveReportGroup(
            adminId,
            requestDto
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

    @Deprecated(
        message = "Use POST /api/admin/report-groups/{reportGroupId}/reject instead."
    )
    @PostMapping("/groups/reject")
    @Operation(
        summary = "레거시 신고 그룹 반려",
        description = "targetType과 targetId 기반으로 신고 그룹을 반려합니다. 신규 API 사용을 권장합니다.",
        deprecated = true
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "레거시 신고 그룹 반려 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "404", description = "처리 가능한 신고 없음")
        ]
    )
    fun rejectGroup(
        @RequestBody
        @Valid
        requestDto: AdminReportRequestDTO,

        @Parameter(hidden = true)
        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        val adminId = getAuthenticatedUserId(principal)

        log.warn(
            "Legacy report group reject API called - adminId={}, targetType={}, targetId={}",
            adminId,
            requestDto.targetType,
            requestDto.reportId
        )

        adminReportService.rejectReportGroup(
            adminId,
            requestDto
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
