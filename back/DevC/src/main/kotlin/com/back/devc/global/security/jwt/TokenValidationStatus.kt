package com.back.devc.global.security.jwt

import com.back.devc.global.exception.errorCode.AuthErrorCode

enum class TokenValidationStatus {
    VALID,
    MISSING,
    EXPIRED,
    MALFORMED,
    UNSUPPORTED,
    INVALID_SIGNATURE,
    INVALID_TOKEN_TYPE;

    fun isValid(): Boolean {
        return this == VALID
    }

    // 토큰 검증 상태를 공통 인증 실패 응답 코드로 매핑
    fun toAuthErrorCode(): AuthErrorCode {
        return when (this) {
            MISSING -> AuthErrorCode.TOKEN_MISSING
            EXPIRED -> AuthErrorCode.TOKEN_EXPIRED
            MALFORMED,
            UNSUPPORTED,
            INVALID_SIGNATURE,
            INVALID_TOKEN_TYPE -> AuthErrorCode.TOKEN_INVALID
            VALID -> AuthErrorCode.UNAUTHORIZED
        }
    }
}