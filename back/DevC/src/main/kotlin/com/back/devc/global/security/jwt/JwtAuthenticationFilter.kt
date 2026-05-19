package com.back.devc.global.security.jwt

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val memberRepository: MemberRepository,

    @Value("\${custom.jwt.access-cookie-name:access_token}")
    private val accessCookieName: String
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return CorsUtils.isPreFlightRequest(request)
    }

    // 모든 요청에서 JWT를 확인하고, 유효한 토큰이면 SecurityContext에 인증 정보를 저장
    // 유효하지 않은 토큰이면 실패 상태를 request attribute에 담아 EntryPoint가 응답 코드로 변환할 수 있게 한다.
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val tokenResolveResult = resolveToken(request)

        request.setAttribute(
            TOKEN_VALIDATION_STATUS_ATTRIBUTE,
            tokenResolveResult.status
        )

        if (!tokenResolveResult.status.isValid()) {
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val token = tokenResolveResult.token
        if (token == null) {
            request.setAttribute(
                TOKEN_VALIDATION_STATUS_ATTRIBUTE,
                TokenValidationStatus.MISSING
            )
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        val userId = jwtProvider.getUserId(token)
        val member = memberRepository.findById(userId).orElse(null)

        if (!isAuthenticatableMember(member)) {
            request.setAttribute(
                TOKEN_VALIDATION_STATUS_ATTRIBUTE,
                TokenValidationStatus.INVALID_TOKEN_TYPE
            )
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        val memberId = member.userId
        if (memberId == null) {
            request.setAttribute(
                TOKEN_VALIDATION_STATUS_ATTRIBUTE,
                TokenValidationStatus.INVALID_TOKEN_TYPE
            )
            SecurityContextHolder.clearContext()
            filterChain.doFilter(request, response)
            return
        }

        val principal = JwtPrincipal(
            memberId,
            member.email,
            member.role.name
        )

        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${member.role.name}"))
        )

        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }

    // 인증 가능한 회원 상태인지 확인
    // 탈퇴 또는 블랙리스트 계정은 유효한 토큰을 가지고 있어도 인증 처리하지 않는다.
    private fun isAuthenticatableMember(member: Member?): Boolean {
        return member != null &&
                (
                        member.status == MemberStatus.ACTIVE ||
                                member.status == MemberStatus.WARNED ||
                                member.status == MemberStatus.SUSPENDED
                        )
    }

    // 요청에서 토큰을 추출하고 검증 결과를 함께 반환
    // Authorization Bearer 토큰을 우선 사용하고, 없으면 access_token 쿠키를 확인
    private fun resolveToken(request: HttpServletRequest): TokenResolveResult {
        val bearerToken = resolveBearerToken(request)

        if (bearerToken != null) {
            val bearerStatus = jwtProvider.validateAccessTokenStatus(bearerToken)
            return TokenResolveResult(bearerToken, bearerStatus)
        }

        val cookieToken = resolveCookieToken(request)

        if (cookieToken != null) {
            val cookieStatus = jwtProvider.validateAccessTokenStatus(cookieToken)
            return TokenResolveResult(cookieToken, cookieStatus)
        }

        return TokenResolveResult(null, TokenValidationStatus.MISSING)
    }

    // Authorization 헤더에서 Bearer 토큰을 추출
    // Bearer 접두어가 없더라도 값이 있으면 잘못된 토큰으로 검증되도록 그대로 반환
    private fun resolveBearerToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader(AUTHORIZATION_HEADER)

        if (authorization.isNullOrBlank()) {
            return null
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            return authorization.trim()
        }

        val token = authorization.substring(BEARER_PREFIX.length).trim()

        return token.ifBlank { null }
    }

    // 요청 쿠키에서 access_token 값을 추출
    // Authorization 헤더가 없을 때 쿠키 기반 인증을 처리하기 위해 사용
    private fun resolveCookieToken(request: HttpServletRequest): String? {
        val cookies = request.cookies

        if (cookies.isNullOrEmpty()) {
            return null
        }

        for (cookie in cookies) {
            if (accessCookieName == cookie.name) {
                val value = cookie.value
                return value?.trim()?.ifBlank { null }
            }
        }

        return null
    }

    // 추출한 토큰과 검증 상태를 함께 전달하기 위한 내부 DTO
    // 필터 흐름에서 토큰 문자열과 실패 원인을 분리하지 않고 함께 다루기 위해 사용
    private data class TokenResolveResult(
        val token: String?,
        val status: TokenValidationStatus
    )

    companion object {
        const val TOKEN_VALIDATION_STATUS_ATTRIBUTE = "tokenValidationStatus"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
