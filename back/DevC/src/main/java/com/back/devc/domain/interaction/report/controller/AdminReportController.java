package com.back.devc.domain.interaction.report.controller;

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO;
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO;
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.service.AdminReportService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.ReportSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /* =========================
       개별 로그 - Raw
    ========================= */
    @GetMapping("/raw")
    public ResponseEntity<SuccessResponse<Page<ReportResponseDTO>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<ReportResponseDTO> reports = adminReportService.getReports(status, pageable);


        ReportSuccessCode successCode = ReportSuccessCode.REPORT_200_LIST;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, reports));
    }

    /* =========================
       GROUPED REPORTS - 같은 타겟에 대한 신고들을 그룹핑하여 조회
    ========================= */
    @GetMapping("/groups")
    public ResponseEntity<SuccessResponse<Page<ReportGroupResponseDTO>>> getGrouped(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "latestCreatedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        Page<ReportGroupResponseDTO> groups = adminReportService.getGroupedReports(status, pageable);

        ReportSuccessCode successCode = ReportSuccessCode.REPORT_200_GROUP_LIST;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, groups));
    }

    /* =========================
       GROUP APPROVE
    ========================= */
    @PostMapping("/groups/approve")
    public ResponseEntity<SuccessResponse<Void>> approveGroup(
            @RequestBody AdminReportRequestDTO requestDto,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        adminReportService.approveReportGroup(getAuthenticatedUserId(principal), requestDto);

        ReportSuccessCode successCode = ReportSuccessCode.REPORT_200_GROUP_APPROVE;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, null));
    }

    /* =========================
       GROUP REJECT
    ========================= */
    @PostMapping("/groups/reject")
    public ResponseEntity<SuccessResponse<Void>> rejectGroup(
            @RequestBody AdminReportRequestDTO requestDto,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        adminReportService.rejectReportGroup(getAuthenticatedUserId(principal), requestDto);

        ReportSuccessCode successCode = ReportSuccessCode.REPORT_200_GROUP_REJECT;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, null));
    }

    private Long getAuthenticatedUserId(JwtPrincipal principal) {
        if (principal == null) throw new ApiException(ErrorCode.UNAUTHORIZED);
        return principal.userId();
    }
}