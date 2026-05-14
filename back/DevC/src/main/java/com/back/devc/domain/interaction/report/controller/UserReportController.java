package com.back.devc.domain.interaction.report.controller;

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO;
import com.back.devc.domain.interaction.report.service.UserReportService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.ReportSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class UserReportController {

    private final UserReportService reportService;

    @PostMapping("/post")
    public ResponseEntity<SuccessResponse<Void>> reportPost(
            @RequestBody @Valid ReportRequestDTO requestDto,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        reportService.reportPost(getAuthenticatedUserId(principal), requestDto);

        ReportSuccessCode successCode = ReportSuccessCode.REPORT_201_POST;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, null));
    }

    @PostMapping("/comment")
    public ResponseEntity<SuccessResponse<Void>> reportComment(
            @RequestBody @Valid ReportRequestDTO requestDto,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        reportService.reportComment(getAuthenticatedUserId(principal), requestDto);

        ReportSuccessCode successCode = ReportSuccessCode.REPORT_201_COMMENT;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, null));
    }

    private Long getAuthenticatedUserId(JwtPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
