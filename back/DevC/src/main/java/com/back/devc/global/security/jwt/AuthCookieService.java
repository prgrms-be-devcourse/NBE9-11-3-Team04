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

    // 로그아웃 시 Access Token 쿠키를 즉시 만료시킨다.
    public void expireAccessTokenCookie(HttpServletResponse response) {
        addAccessTokenCookie(response, "", 0);
    }

    // Access Token 쿠키를 생성해 Set-Cookie 헤더로 응답에 추가한다.
    private void addAccessTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(accessCookieName, token == null ? "" : token)
                .httpOnly(true)
                .secure(accessCookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(accessCookieSameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Access Token 만료 쿠키 헤더 문자열을 생성한다.
    public String buildExpiredAccessTokenCookieHeader() {
        return ResponseCookie.from(accessCookieName, "")
                .httpOnly(true)
                .secure(accessCookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(accessCookieSameSite)
                .build()
                .toString();
    }
}