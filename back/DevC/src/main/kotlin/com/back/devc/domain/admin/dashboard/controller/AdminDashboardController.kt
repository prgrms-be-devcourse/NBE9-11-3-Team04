package com.back.devc.domain.admin.dashboard.controller

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto
import com.back.devc.domain.admin.dashboard.service.AdminDashboardService
import com.back.devc.global.response.SuccessCode
import com.back.devc.global.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService,
) {

    @GetMapping
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
