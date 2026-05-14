package com.back.devc.global.security.jwt;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_VALIDATION_STATUS_ATTRIBUTE = "tokenValidationStatus";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Value("${custom.jwt.access-cookie-name:access_token}")
    private String accessCookieName;

    // 모든 요청에서 JWT를 확인하고, 유효한 토큰이면 SecurityContext에 인증 정보를 저장
    // 유효하지 않은 토큰이면 실패 상태를 request attribute에 남겨 EntryPoint가 응답 코드로 변환할 수 있게 한다.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        TokenResolveResult tokenResolveResult = resolveToken(request);

        request.setAttribute(
                TOKEN_VALIDATION_STATUS_ATTRIBUTE,
                tokenResolveResult.status()
        );

        if (!tokenResolveResult.status().isValid()) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = tokenResolveResult.token();
        Long userId = jwtProvider.getUserId(token);

        Member member = memberRepository.findById(userId).orElse(null);

        if (!isAuthenticatableMember(member)) {
            request.setAttribute(
                    TOKEN_VALIDATION_STATUS_ATTRIBUTE,
                    TokenValidationStatus.INVALID_TOKEN_TYPE
            );
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        JwtPrincipal principal = new JwtPrincipal(
                member.getUserId(),
                member.getEmail(),
                member.getRole().name()
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    // 인증 가능한 회원 상태인지 확인
    // 탈퇴 또는 블랙리스트 계정은 유효한 토큰을 가지고 있어도 인증 처리하지 않는다.
    private boolean isAuthenticatableMember(Member member) {
        return member != null &&
                (
                        member.getStatus() == MemberStatus.ACTIVE ||
                                member.getStatus() == MemberStatus.WARNED ||
                                member.getStatus() == MemberStatus.SUSPENDED
                );
    }

    // 요청에서 토큰을 추출하고 검증 결과를 함께 반환
    // Authorization Bearer 토큰을 우선 사용하고, 없으면 access_token 쿠키를 확인
    private TokenResolveResult resolveToken(HttpServletRequest request) {
        String bearerToken = resolveBearerToken(request);

        if (bearerToken != null) {
            TokenValidationStatus bearerStatus =
                    jwtProvider.validateAccessTokenStatus(bearerToken);

            return new TokenResolveResult(bearerToken, bearerStatus);
        }

        String cookieToken = resolveCookieToken(request);

        if (cookieToken != null) {
            TokenValidationStatus cookieStatus =
                    jwtProvider.validateAccessTokenStatus(cookieToken);

            return new TokenResolveResult(cookieToken, cookieStatus);
        }

        return new TokenResolveResult(null, TokenValidationStatus.MISSING);
    }

    // Authorization 헤더에서 Bearer 토큰을 추출
    // Bearer 접두사가 없더라도 값이 있으면 잘못된 토큰으로 검증되도록 그대로 반환
    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            return authorization.trim();
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();

        return token.isBlank() ? null : token;
    }

    // 요청 쿠키에서 access_token 값을 추출
    // Authorization 헤더가 없을 때 쿠키 기반 인증을 처리하기 위해 사용
    private String resolveCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie == null) {
                continue;
            }

            if (accessCookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value.trim();
            }
        }

        return null;
    }

    // 추출한 토큰과 검증 상태를 함께 전달하기 위한 내부 DTO
    // 필터 흐름에서 토큰 문자열과 실패 원인을 분리하지 않고 함께 다루기 위해 사용
    private record TokenResolveResult(
            String token,
            TokenValidationStatus status
    ) {
    }
}