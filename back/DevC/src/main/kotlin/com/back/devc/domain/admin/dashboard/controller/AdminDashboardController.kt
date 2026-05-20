package com.back.devc.domain.admin.dashboard.controller

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto
import com.back.devc.domain.admin.dashboard.service.AdminDashboardService
import com.back.devc.global.response.SuccessCode
import com.back.devc.global.response.SuccessResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 대시보드 API", description = "관리자 대시보드 요약 통계 조회 API")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService,
) {

    @GetMapping
    @Operation(summary = "관리자 대시보드 조회", description = "전체 회원/게시글/신고와 오늘 활동 통계를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "관리자 대시보드 조회 성공"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음")
        ]
    )
    fun getDashboard(): ResponseEntity<SuccessResponse<DashboardResponseDto>> {
        return ResponseEntity
            .status(SuccessCode.DASHBOARD_LIST.status)
            .body(
                SuccessResponse.of(
                    SuccessCode.DASHBOARD_LIST,
                    adminDashboardService.getDashboardData(),
                ),
            )
    }
}
