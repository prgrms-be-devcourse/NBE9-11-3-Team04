package com.back.devc.global.security.jwt;

import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;

public final class JwtPrincipalHelper {

    private JwtPrincipalHelper() {
    }

    /**
     * 컨트롤러에서 공통으로 사용하는 로그인 사용자 식별 메서드
     *
     * JwtAuthenticationFilter가 정상적으로 principal을 세팅한 경우 userId를 반환하고,
     * 인증 정보가 없으면 인증 실패로 간주해 UNAUTHORIZED 예외를 반환
     */
    public static Long getAuthenticatedUserId(JwtPrincipal principal) {
        // 토큰이 없거나 필터에서 principal을 세팅하지 못한 요청은 인증 실패로 처리
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
