package com.back.devc.global.security.jwt;

import com.back.devc.domain.member.member.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;

    public JwtProvider(
            @Value("${custom.jwt.secret-key}") String secretKey,
            @Value("${custom.jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public String createAccessToken(Member member) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .subject(String.valueOf(member.getUserId()))
                .claim("tokenType", "ACCESS")
                .claim("email", member.getEmail())
                .claim("role", member.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    // 토큰 서명/만료/형식을 검증한다.
    public boolean validateToken(String token) {
        return validateTokenStatus(token).isValid();
    }

    public TokenValidationStatus validateAccessTokenStatus(String token) {
        TokenValidationStatus tokenStatus = validateTokenStatus(token);
        if (!tokenStatus.isValid()) {
            return tokenStatus;
        }

        String tokenType = parseClaims(token).get("tokenType", String.class);
        if (!"ACCESS".equals(tokenType)) {
            return TokenValidationStatus.INVALID_TOKEN_TYPE;
        }

        return TokenValidationStatus.VALID;
    }

    public TokenValidationStatus validateTokenStatus(String token) {
        if (token == null || token.isBlank()) {
            return TokenValidationStatus.MISSING;
        }

        try {
            parseClaims(token);
            return TokenValidationStatus.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationStatus.EXPIRED;
        } catch (MalformedJwtException | IllegalArgumentException e) {
            return TokenValidationStatus.MALFORMED;
        } catch (UnsupportedJwtException e) {
            return TokenValidationStatus.UNSUPPORTED;
        } catch (SignatureException | SecurityException e) {
            return TokenValidationStatus.INVALID_SIGNATURE;
        } catch (JwtException e) {
            return TokenValidationStatus.MALFORMED;
        }
    }

    // subject(userId) 클레임을 Long으로 변환해 반환한다.
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 이메일 클레임을 반환한다.
    public String getEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    // 권한(role) 클레임을 반환한다.
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 서명 검증까지 포함해 JWT payload(claims)를 파싱한다.
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
