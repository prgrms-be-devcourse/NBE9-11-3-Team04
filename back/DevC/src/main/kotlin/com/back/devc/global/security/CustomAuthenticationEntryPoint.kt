package com.back.devc.global.security

import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.AuthErrorCode
import com.back.devc.global.security.jwt.JwtAuthenticationFilter
import com.back.devc.global.security.jwt.TokenValidationStatus
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import java.io.IOException

@Component
class CustomAuthenticationEntryPoint(
    // Security 필터 단계에서 발생한 인증 예외를 전역 예외 처리기로 위임한다.
    private val handlerExceptionResolver: HandlerExceptionResolver
) : AuthenticationEntryPoint {

    // 미인증 요청(401) 발생 시 공통 ErrorResponse 포맷으로 응답하도록 연결한다.
    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val errorCode = resolveAuthErrorCode(request)

        handlerExceptionResolver.resolveException(
            request,
            response,
            null,
            ApiException(errorCode)
        )
    }

    // JwtAuthenticationFilter가 request attribute에 저장한 토큰 검증 상태를 읽어
    // 클라이언트에 내려줄 인증 실패 에러 코드로 변환한다.
    // 상태값이 없으면 보호 API에 토큰 없이 접근한 상황으로 보고 TOKEN_MISSING을 반환한다.
    private fun resolveAuthErrorCode(request: HttpServletRequest): AuthErrorCode {
        val statusAttribute = request.getAttribute(
            JwtAuthenticationFilter.TOKEN_VALIDATION_STATUS_ATTRIBUTE
        )

        if (statusAttribute is TokenValidationStatus) {
            return statusAttribute.toAuthErrorCode()
        }

        return AuthErrorCode.TOKEN_MISSING
    }
}