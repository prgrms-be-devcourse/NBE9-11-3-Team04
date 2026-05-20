package com.back.devc.domain.member.member.controller

import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse
import com.back.devc.domain.member.member.dto.AdmMemberListRequest
import com.back.devc.domain.member.member.dto.AdmMemberListResponse
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.service.AdmMemberService
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.MemberSuccessCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/members")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 회원 API", description = "관리자용 회원 목록, 상세, 상태 변경 API")
class AdmMemberController(
    private val adminMemberService: AdmMemberService,
) {

    @GetMapping
    @Operation(summary = "관리자 회원 목록 조회", description = "관리자가 회원 목록을 페이지, 키워드, 상태 조건으로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "관리자 회원 목록 조회 성공"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음")
        ]
    )
    fun getMembers(
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20") size: Int,

        @Parameter(description = "이메일 또는 닉네임 검색어", example = "dev")
        @RequestParam(required = false) keyword: String?,

        @Parameter(description = "회원 상태", example = "ACTIVE")
        @RequestParam(required = false) status: MemberStatus?,
    ): ResponseEntity<SuccessResponse<Page<AdmMemberListResponse>>> {
        val request = AdmMemberListRequest(
            page = page,
            size = size,
            keyword = keyword,
            status = status,
        )
        val response = adminMemberService.getMembers(request)
        val successCode = MemberSuccessCode.ADMIN_MEMBER_LIST_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping("/{userId}")
    @Operation(summary = "관리자 회원 상세 조회", description = "관리자가 특정 회원의 상세 정보와 활동 수를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "관리자 회원 상세 조회 성공"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
        ]
    )
    fun getMemberDetail(
        @Parameter(description = "회원 ID", example = "1")
        @PathVariable userId: Long,
    ): ResponseEntity<SuccessResponse<AdmMemberDetailResponse>> {
        val response = adminMemberService.getMemberDetail(userId)
        val successCode = MemberSuccessCode.ADMIN_MEMBER_DETAIL_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "관리자 회원 상태 변경", description = "관리자가 회원 상태를 변경하고, 정지 상태인 경우 정지 기간을 지정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "관리자 회원 상태 변경 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 상태 변경 요청"),
            ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
        ]
    )
    fun updateMemberStatus(
        @Parameter(description = "회원 ID", example = "1")
        @PathVariable userId: Long,

        @Valid @RequestBody request: AdmMemberStatusUpdateRequest,
    ): ResponseEntity<SuccessResponse<AdmMemberDetailResponse>> {
        val response = adminMemberService.updateMemberStatus(userId, request)
        val successCode = MemberSuccessCode.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }
}

