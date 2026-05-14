package com.back.devc.domain.auth.controller;

import com.back.devc.domain.auth.dto.login.LoginRequest;
import com.back.devc.domain.auth.dto.login.LoginResponse;
import com.back.devc.domain.auth.dto.logout.LogoutResponse;
import com.back.devc.domain.auth.dto.signup.SignUpRequest;
import com.back.devc.domain.auth.dto.signup.SignUpResponse;
import com.back.devc.domain.auth.service.AuthService;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.AuthSuccessCode;
import com.back.devc.global.security.jwt.AuthCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @Value("${custom.jwt.access-token-expiration-seconds:3600}")
    private long accessTokenExpirationSeconds;

    // 로그아웃 처리 후 액세스 토큰 쿠키를 만료시키고 성공 응답을 반환한다.
    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<LogoutResponse>> logout(HttpServletResponse response) {
        LogoutResponse body = authService.logout();
        authCookieService.setAccessTokenCookie(response, "", 0);

        AuthSuccessCode successCode = AuthSuccessCode.AUTH_200_LOGOUT_SUCCESS;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }

    // 이메일/비밀번호 로그인 후 토큰 쿠키를 설정하고 로그인 정보를 반환한다.
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResponse body = authService.login(request);
        authCookieService.setAccessTokenCookie(response, body.accessToken(), accessTokenExpirationSeconds);

        AuthSuccessCode successCode = AuthSuccessCode.AUTH_200_LOGIN_SUCCESS;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }

    // 로컬 회원가입을 처리하고 생성된 사용자 정보를 반환한다.
    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse body = authService.signUp(request);

        AuthSuccessCode successCode = AuthSuccessCode.AUTH_201_SIGNUP_SUCCESS;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, body));
    }
}