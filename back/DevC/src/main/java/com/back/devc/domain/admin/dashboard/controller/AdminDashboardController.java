package com.back.devc.domain.admin.dashboard.controller;

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto;
import com.back.devc.domain.admin.dashboard.service.AdminDashboardService;
import com.back.devc.global.response.SuccessCode;
import com.back.devc.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public ResponseEntity<SuccessResponse<DashboardResponseDto>> getDashboard() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(SuccessResponse.of(SuccessCode.DASHBOARD_LIST, adminDashboardService.getDashboardData()
                ));
    }
}