package com.back.devc.domain.member.member.controller

import com.back.devc.domain.member.member.dto.MemberWithdrawResponse
import com.back.devc.domain.member.member.dto.MyInfoResponse
import com.back.devc.domain.member.member.dto.PublicProfileResponse
import com.back.devc.domain.member.member.service.MemberService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.MemberSuccessCode
import com.back.devc.global.security.jwt.AuthCookieService
import com.back.devc.global.security.jwt.JwtPrincipal
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class MemberController(
    private val memberService: MemberService,
    private val authCookieService: AuthCookieService
) {

    // 내 정보 조회
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal principal: JwtPrincipal?
    ): ResponseEntity<SuccessResponse<MyInfoResponse>> {
        if (principal == null) {
            throw ApiException(ErrorCode.UNAUTHORIZED)
        }

        val body = memberService.getMyInfo(principal.userId())
        val successCode = MemberSuccessCode.MEMBER_200_ME_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }

    // 공개 프로필 조회
    @GetMapping("/{userId}/profile")
    fun getPublicProfile(
        @PathVariable userId: Long
    ): ResponseEntity<SuccessResponse<PublicProfileResponse>> {
        val body = memberService.getPublicProfile(userId)
        val successCode = MemberSuccessCode.MEMBER_200_PUBLIC_PROFILE_GET_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    fun withdraw(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        response: HttpServletResponse
    ): ResponseEntity<SuccessResponse<MemberWithdrawResponse>> {
        if (principal == null) {
            throw ApiException(ErrorCode.UNAUTHORIZED)
        }

        val body = memberService.withdraw(principal.userId())
        SecurityContextHolder.clearContext()
        authCookieService.expireAccessTokenCookie(response)

        val successCode = MemberSuccessCode.MEMBER_200_WITHDRAW_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }
}