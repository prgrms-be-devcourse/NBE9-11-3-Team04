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

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Value("${custom.jwt.access-cookie-name:access_token}")
    private String accessCookieName;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtProvider.validateAccessTokenStatus(token) == TokenValidationStatus.VALID) {

            Long userId = jwtProvider.getUserId(token);
            Member member = memberRepository.findById(userId).orElse(null);

            if (!isAuthenticatableMember(member)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String email = member.getEmail();
            String role = member.getRole().name();

            JwtPrincipal principal = new JwtPrincipal(userId, email, role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // 회원이 존재하고, 상태가 ACTIVE, WARNED, SUSPENDED 중 하나인지 확인
    // 기존: ACTIVE만 가능에서 기능 체크를 위해 수정하였습니다.
    private boolean isAuthenticatableMember(Member member) {
        return member != null &&
                (member.getStatus() == MemberStatus.ACTIVE
                        || member.getStatus() == MemberStatus.WARNED || member.getStatus() == MemberStatus.SUSPENDED);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = resolveBearerToken(request);
        if (isValidAccessToken(bearerToken)) {
            return bearerToken;
        }

        String cookieToken = resolveCookieToken(request);
        if (isValidAccessToken(cookieToken)) {
            return cookieToken;
        }

        return null;
    }

    private boolean isValidAccessToken(String token) {
        return token != null
                && jwtProvider.validateAccessTokenStatus(token) == TokenValidationStatus.VALID;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isBlank() ? null : token;
    }

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
                return (value == null || value.isBlank()) ? null : value.trim();
            }
        }

        return null;
    }
}