package com.back.devc.domain.interaction.report.controller

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.service.UserReportService
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/report")
@Tag(name = "신고 API", description = "사용자 신고 접수 API")
class UserReportController(
    private val reportService: UserReportService
) {

    @PostMapping("/post")
    @Operation(summary = "게시글 신고", description = "로그인한 사용자가 다른 사용자의 게시글을 신고합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "게시글 신고 접수 성공"),
            ApiResponse(responseCode = "400", description = "자기 게시글 신고 등 잘못된 요청"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "409", description = "이미 신고한 대상")
        ]
    )
    fun reportPost(
        @RequestBody @Valid
        requestDto: ReportRequestDTO,

        @Parameter(hidden = true)
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
    @Operation(summary = "댓글 신고", description = "로그인한 사용자가 다른 사용자의 댓글을 신고합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "댓글 신고 접수 성공"),
            ApiResponse(responseCode = "400", description = "자기 댓글 신고 등 잘못된 요청"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "409", description = "이미 신고한 대상")
        ]
    )
    fun reportComment(
        @RequestBody @Valid
        requestDto: ReportRequestDTO,

        @Parameter(hidden = true)
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

