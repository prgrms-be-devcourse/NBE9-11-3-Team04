package com.back.devc.global.security.jwt;

import com.back.devc.global.exception.errorCode.AuthErrorCode;

public enum TokenValidationStatus {
    VALID,
    MISSING,
    EXPIRED,
    MALFORMED,
    UNSUPPORTED,
    INVALID_SIGNATURE,
    INVALID_TOKEN_TYPE;

    public boolean isValid() {
        return this == VALID;
    }

    // 토큰 검증 상태를 공통 인증 실패 응답 코드로 매핑
    public AuthErrorCode toAuthErrorCode() {
        return switch (this) {
            case MISSING -> AuthErrorCode.TOKEN_MISSING;
            case EXPIRED -> AuthErrorCode.TOKEN_EXPIRED;
            case MALFORMED, UNSUPPORTED, INVALID_SIGNATURE, INVALID_TOKEN_TYPE ->
                    AuthErrorCode.TOKEN_INVALID;
            case VALID -> AuthErrorCode.UNAUTHORIZED;
        };
    }
}
