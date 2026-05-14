package com.back.devc.global.security;

import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Security 필터 단계에서 발생한 인증 예외를 전역 예외 처리기로 위임한다.
    private final HandlerExceptionResolver handlerExceptionResolver;

    // 미인증 요청(401) 발생 시 공통 ErrorResponse 포맷으로 응답하도록 연결한다.
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        handlerExceptionResolver.resolveException(
                request,
                response,
                null,
                new ApiException(ErrorCode.UNAUTHORIZED)
        );
    }
}
