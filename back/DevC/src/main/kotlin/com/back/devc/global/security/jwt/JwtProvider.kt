package com.back.devc.global.security.jwt

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.AuthErrorCode
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${custom.jwt.secret-key}")
    secretKey: String,

    @Value("\${custom.jwt.access-token-expiration-seconds}")
    private val accessTokenExpirationSeconds: Long
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))

    // 로그인 성공 시 회원 정보를 기반으로 Access Token을 생성
    // 토큰에는 사용자 식별값, 이메일, 권한, 토큰 타입, 만료 시간이 포함
    fun createAccessToken(member: Member): String {
        val now = Instant.now()
        val expiry = now.plusSeconds(accessTokenExpirationSeconds)
        val userId = member.userId ?: throw ApiException(AuthErrorCode.UNAUTHORIZED)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("tokenType", ACCESS_TOKEN_TYPE)
            .claim("email", member.email)
            .claim("role", member.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact()
    }

    // 토큰 서명/만료/형식을 검증한다.
    fun validateToken(token: String?): Boolean {
        return validateTokenStatus(token).isValid()
    }

    // Access Token으로 사용할 수 있는 토큰인지 검증
    // 서명/만료/형식 검증을 먼저 수행하고, 이후 tokenType이 ACCESS인지 확인
    fun validateAccessTokenStatus(token: String?): TokenValidationStatus {
        val tokenStatus = validateTokenStatus(token)
        if (!tokenStatus.isValid()) {
            return tokenStatus
        }

        val accessToken = token ?: return TokenValidationStatus.MISSING
        val tokenType = parseClaims(accessToken).get("tokenType", String::class.java)
        if (ACCESS_TOKEN_TYPE != tokenType) {
            return TokenValidationStatus.INVALID_TOKEN_TYPE
        }

        return TokenValidationStatus.VALID
    }

    // JWT 검증 결과를 TokenValidationStatus로 세분화해 반환
    // 인증 필터와 EntryPoint가 이 상태값을 기준으로 응답 코드를 결정
    fun validateTokenStatus(token: String?): TokenValidationStatus {
        if (token.isNullOrBlank()) {
            return TokenValidationStatus.MISSING
        }

        return try {
            parseClaims(token)
            TokenValidationStatus.VALID
        } catch (e: ExpiredJwtException) {
            TokenValidationStatus.EXPIRED
        } catch (e: MalformedJwtException) {
            TokenValidationStatus.MALFORMED
        } catch (e: IllegalArgumentException) {
            TokenValidationStatus.MALFORMED
        } catch (e: UnsupportedJwtException) {
            TokenValidationStatus.UNSUPPORTED
        } catch (e: SignatureException) {
            TokenValidationStatus.INVALID_SIGNATURE
        } catch (e: SecurityException) {
            TokenValidationStatus.INVALID_SIGNATURE
        } catch (e: JwtException) {
            TokenValidationStatus.MALFORMED
        }
    }

    // subject(userId) 클레임을 Long으로 변환해 반환한다.
    fun getUserId(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    // 이메일 클레임을 반환한다.
    fun getEmail(token: String): String {
        return parseClaims(token).get("email", String::class.java)
    }

    // 권한(role) 클레임을 반환한다.
    fun getRole(token: String): String {
        return parseClaims(token).get("role", String::class.java)
    }

    // 서명 검증까지 포함해 JWT payload(claims)를 파싱한다.
    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    companion object {
        private const val ACCESS_TOKEN_TYPE = "ACCESS"
    }
}