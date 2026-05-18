package com.back.devc.global.security.jwt

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class AuthCookieService(
    @Value("\${custom.jwt.access-cookie-name:access_token}")
    private val accessCookieName: String,

    @Value("\${custom.jwt.access-cookie-secure:false}")
    private val accessCookieSecure: Boolean,

    @Value("\${custom.jwt.access-cookie-same-site:Lax}")
    private val accessCookieSameSite: String,

    @Value("\${custom.jwt.access-token-expiration-seconds:3600}")
    private val accessTokenExpirationSeconds: Long
) {

    // 로그인 성공 시 Access Token을 HttpOnly Cookie로 응답에 추가한다.
    fun setAccessTokenCookie(response: HttpServletResponse, token: String?) {
        addAccessTokenCookie(response, token, accessTokenExpirationSeconds)
    }

    // 로그아웃 또는 회원탈퇴 시 Access Token 쿠키를 즉시 만료시킨다.
    fun expireAccessTokenCookie(response: HttpServletResponse) {
        addAccessTokenCookie(response, "", 0)
    }

    // Access Token 쿠키를 Set-Cookie 헤더로 응답에 추가
    // 실제 쿠키 생성은 buildAccessTokenCookie()에 위임
    private fun addAccessTokenCookie(
        response: HttpServletResponse,
        token: String?,
        maxAgeSeconds: Long
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildAccessTokenCookie(token, maxAgeSeconds))
    }

    // Access Token 쿠키 문자열을 생성
    // HttpOnly, Secure, Path, Max-Age, SameSite 등 쿠키 정책을 한 곳에서 관리
    private fun buildAccessTokenCookie(token: String?, maxAgeSeconds: Long): String {
        return ResponseCookie.from(accessCookieName, token ?: "")
            .httpOnly(true)
            .secure(accessCookieSecure)
            .path("/")
            .maxAge(maxAgeSeconds)
            .sameSite(accessCookieSameSite)
            .build()
            .toString()
    }
}