package com.back.devc.domain.member.member.controller

import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse
import com.back.devc.domain.member.member.dto.AdmMemberListRequest
import com.back.devc.domain.member.member.dto.AdmMemberListResponse
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.service.AdmMemberService
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.MemberSuccessCode
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
class AdmMemberController(
    private val adminMemberService: AdmMemberService,
) {

    @GetMapping
    fun getMembers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) keyword: String?,
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
    fun getMemberDetail(
        @PathVariable userId: Long,
    ): ResponseEntity<SuccessResponse<AdmMemberDetailResponse>> {
        val response = adminMemberService.getMemberDetail(userId)
        val successCode = MemberSuccessCode.ADMIN_MEMBER_DETAIL_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @PatchMapping("/{userId}/status")
    fun updateMemberStatus(
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

