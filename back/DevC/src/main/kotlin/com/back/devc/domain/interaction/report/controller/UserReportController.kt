package com.back.devc.domain.interaction.report.controller

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.service.UserReportService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.ReportSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/report")
class UserReportController(
    private val reportService: UserReportService
) {

    @PostMapping("/post")
    fun reportPost(
        @RequestBody @Valid
        requestDto: ReportRequestDTO,

        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        reportService.reportPost(
            getAuthenticatedUserId(principal),
            requestDto
        )

        val successCode = ReportSuccessCode.REPORT_201_POST

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, null))
    }

    @PostMapping("/comment")
    fun reportComment(
        @RequestBody @Valid
        requestDto: ReportRequestDTO,

        @AuthenticationPrincipal
        principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<Void>> {

        reportService.reportComment(
            getAuthenticatedUserId(principal),
            requestDto
        )

        val successCode = ReportSuccessCode.REPORT_201_COMMENT

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, null))
    }

    private fun getAuthenticatedUserId(principal: JwtPrincipal?): Long {
        return principal?.userId
            ?: throw ApiException(ErrorCode.UNAUTHORIZED)
    }
}

