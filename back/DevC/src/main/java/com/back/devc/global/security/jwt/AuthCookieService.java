package com.back.devc.global.security.jwt;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    @Value("${custom.jwt.access-cookie-name:access_token}")
    private String accessCookieName;

    @Value("${custom.jwt.access-cookie-secure:false}")
    private boolean accessCookieSecure;

    @Value("${custom.jwt.access-cookie-same-site:Lax}")
    private String accessCookieSameSite;

    @Value("${custom.jwt.access-token-expiration-seconds:3600}")
    private long accessTokenExpirationSeconds;

    // 로그인 성공 시 Access Token을 HttpOnly Cookie로 응답에 추가한다.
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        addAccessTokenCookie(response, token, accessTokenExpirationSeconds);
    }

    // 로그아웃 또는 회원탈퇴 시 Access Token 쿠키를 즉시 만료시킨다.
    public void expireAccessTokenCookie(HttpServletResponse response) {
        addAccessTokenCookie(response, "", 0);
    }

    // Access Token 쿠키를 Set-Cookie 헤더로 응답에 추가
    // 실제 쿠키 생성은 buildAccessTokenCookie()에 위임
    private void addAccessTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(token, maxAgeSeconds));
    }

    // Access Token 쿠키 문자열을 생성
    // HttpOnly, Secure, Path, Max-Age, SameSite 등 쿠키 정책을 한 곳에서 관리
    private String buildAccessTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(accessCookieName, token == null ? "" : token)
                .httpOnly(true)
                .secure(accessCookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(accessCookieSameSite)
                .build()
                .toString();
    }
}