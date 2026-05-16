package com.back.devc.domain.auth.controller

import com.back.devc.domain.auth.dto.login.LoginRequest
import com.back.devc.domain.auth.dto.login.LoginResponse
import com.back.devc.domain.auth.dto.logout.LogoutResponse
import com.back.devc.domain.auth.dto.signup.SignUpRequest
import com.back.devc.domain.auth.dto.signup.SignUpResponse
import com.back.devc.domain.auth.service.AuthService
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.AuthSuccessCode
import com.back.devc.global.security.jwt.AuthCookieService
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val authCookieService: AuthCookieService
) {

    // 로그아웃 처리 후 액세스 토큰 쿠키를 만료시키고 성공 응답을 반환한다.
    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<SuccessResponse<LogoutResponse>> {
        val body = authService.logout()
        authCookieService.expireAccessTokenCookie(response)

        val successCode = AuthSuccessCode.AUTH_200_LOGOUT_SUCCESS
        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }

    // 이메일/비밀번호 로그인 후 액세스 토큰 쿠키를 설정하고 로그인 정보를 반환한다.
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<SuccessResponse<LoginResponse>> {
        val body = authService.login(request)
        authCookieService.setAccessTokenCookie(response, body.accessToken)

        val successCode = AuthSuccessCode.AUTH_200_LOGIN_SUCCESS
        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }

    // 로컬 회원가입을 처리하고 생성된 사용자 정보를 반환한다.
    @PostMapping("/signup")
    fun signUp(
        @Valid @RequestBody request: SignUpRequest
    ): ResponseEntity<SuccessResponse<SignUpResponse>> {
        val body = authService.signUp(request)

        val successCode = AuthSuccessCode.AUTH_201_SIGNUP_SUCCESS
        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, body))
    }
}