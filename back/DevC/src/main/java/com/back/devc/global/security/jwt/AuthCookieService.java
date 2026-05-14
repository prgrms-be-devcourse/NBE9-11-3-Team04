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

    public void setAccessTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(accessCookieName, token == null ? "" : token)
                .httpOnly(true)
                .secure(accessCookieSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(accessCookieSameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

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