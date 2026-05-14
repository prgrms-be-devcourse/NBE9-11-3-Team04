package com.back.devc.domain.member.member.controller;

import com.back.devc.domain.member.member.dto.MemberWithdrawResponse;
import com.back.devc.domain.member.member.dto.MyInfoResponse;
import com.back.devc.domain.member.member.dto.PublicProfileResponse;
import com.back.devc.domain.member.member.service.MemberService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.MemberSuccessCode;
import com.back.devc.global.security.jwt.AuthCookieService;
import com.back.devc.global.security.jwt.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class MemberController {

    private final MemberService memberService;
    private final AuthCookieService authCookieService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<MyInfoResponse>> me(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        MyInfoResponse body = memberService.getMyInfo(principal.userId());
        MemberSuccessCode successCode = MemberSuccessCode.MEMBER_200_ME_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }

    // 유저 프로필 조회
    @GetMapping("/{userId}/profile")
    public ResponseEntity<SuccessResponse<PublicProfileResponse>> getPublicProfile(
            @PathVariable Long userId
    ) {
        PublicProfileResponse body = memberService.getPublicProfile(userId);
        MemberSuccessCode successCode = MemberSuccessCode.MEMBER_200_PUBLIC_PROFILE_GET_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<SuccessResponse<MemberWithdrawResponse>> withdraw(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        MemberWithdrawResponse body = memberService.withdraw(principal.userId());
        SecurityContextHolder.clearContext();

        MemberSuccessCode successCode = MemberSuccessCode.MEMBER_200_WITHDRAW_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .header(HttpHeaders.SET_COOKIE, authCookieService.buildExpiredAccessTokenCookieHeader())
                .body(SuccessResponse.of(successCode, body));
    }
}