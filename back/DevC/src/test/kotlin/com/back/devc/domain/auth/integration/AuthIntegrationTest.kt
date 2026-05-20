package com.back.devc.domain.auth.integration

import com.back.devc.domain.auth.controller.AuthController
import com.back.devc.domain.member.member.controller.MemberController
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.global.security.jwt.JwtProvider
import com.jayway.jsonpath.JsonPath
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import java.util.Date

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("로그인/회원가입 통합 테스트")
internal class AuthIntegrationTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Value("\${custom.jwt.secret-key}")
    private lateinit var jwtSecretKey: String

    @Test
    @DisplayName("회원가입 후 로그인하고 내 정보를 조회한 뒤 로그아웃한다")
    fun signUpThenLoginThenGetMeThenLogout() {
        val unique = uniqueValue("auth-flow")
        val email = "$unique@test.com"
        val password = "password123!"
        val nickname = unique

        mvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password",
                      "nickname": "$nickname"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("signUp"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("AUTH_201_SIGNUP_SUCCESS"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.userId").isNumber)
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))

        val savedMember = memberRepository.findByEmail(email).orElseThrow()

        assertThat(savedMember.nickname).isEqualTo(nickname)
        assertThat(savedMember.passwordHash).isNotEqualTo(password)
        assertThat(passwordEncoder.matches(password, savedMember.passwordHash)).isTrue()

        val loginResponseBody = mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("login"))
            .andExpect(status().isOk)
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")),
            )
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")),
            )
            .andExpect(jsonPath("$.code").value("AUTH_200_LOGIN_SUCCESS"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.userId").value(savedMember.userId))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andReturn()
            .response
            .contentAsString

        val accessToken = JsonPath.read<String>(loginResponseBody, "$.data.accessToken")

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(handler().handlerType(MemberController::class.java))
            .andExpect(handler().methodName("me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.userId").value(savedMember.userId))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))

        mvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("logout"))
            .andExpect(status().isOk)
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")),
            )
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")),
            )
            .andExpect(jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.message").isNotEmpty)
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 실패한다")
    fun signUpFailWhenEmailAlreadyExists() {
        val unique = uniqueValue("duplicate-email")
        val email = "$unique@test.com"
        val password = "password123!"
        val member = createTestMember(email, password, unique)

        memberRepository.saveAndFlush(member)

        mvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password",
                      "nickname": "newNickname"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("signUp"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("AUTH_409_EMAIL"))
            .andExpect(jsonPath("$.timestamp").exists())

        val savedMemberCount = memberRepository.findAll()
            .count { saved -> email == saved.email }

        assertThat(savedMemberCount).isEqualTo(1)
    }

    @Test
    @DisplayName("이미 가입된 닉네임으로 회원가입하면 실패한다")
    fun signUpFailWhenNicknameAlreadyExists() {
        val unique = uniqueValue("duplicate-nickname")
        val nickname = unique
        val password = "password123!"
        val member = createTestMember("$unique@test.com", password, nickname)

        memberRepository.saveAndFlush(member)

        mvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "new-$unique@test.com",
                      "password": "$password",
                      "nickname": "$nickname"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("signUp"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("AUTH_409_NICKNAME"))
            .andExpect(jsonPath("$.timestamp").exists())

        val savedMemberCount = memberRepository.findAll()
            .count { saved -> nickname == saved.nickname }

        assertThat(savedMemberCount).isEqualTo(1)
    }

    @Test
    @DisplayName("잘못된 회원가입 요청값이면 실패한다")
    fun signUpFailWhenRequestInvalid() {
        mvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "",
                      "password": "",
                      "nickname": ""
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("signUp"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("COMMON_400"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.validation").exists())
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 실패한다")
    fun loginFailWhenEmailNotFound() {
        mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "not-found-auth@test.com",
                      "password": "password123!"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("login"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("AUTH_404_EMAIL_NOT_FOUND"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    fun loginFailWhenPasswordMismatch() {
        val unique = uniqueValue("password-mismatch")
        val email = "$unique@test.com"
        val rawPassword = "password123!"
        val member = createTestMember(email, rawPassword, unique)

        memberRepository.saveAndFlush(member)

        mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "wrongPassword123!"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("login"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_PASSWORD_MISMATCH"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("블랙리스트 회원은 로그인할 수 없다")
    fun loginFailWhenMemberBlacklisted() {
        val unique = uniqueValue("blacklisted-auth")
        val email = "$unique@test.com"
        val rawPassword = "password123!"
        val member = createTestMember(email, rawPassword, unique)

        member.updateStatus(MemberStatus.BLACKLISTED)
        memberRepository.saveAndFlush(member)

        mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$rawPassword"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("login"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("AUTH_403_MEMBER_BLACKLISTED"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("삭제된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    fun meFailWhenMemberDeletedAfterLogin() {
        val unique = uniqueValue("deleted-after-login")
        val email = "$unique@test.com"
        val rawPassword = "password123!"
        val nickname = unique
        val member = createTestMember(email, rawPassword, nickname)
        val savedMember = memberRepository.saveAndFlush(member)
        val accessToken = loginAndGetAccessToken(email, rawPassword)

        memberRepository.deleteById(requireNotNull(savedMember.userId))
        memberRepository.flush()
        entityManager.clear()

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("블랙리스트 처리된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    fun meFailWhenMemberBlacklistedAfterLogin() {
        val unique = uniqueValue("blacklisted-after-login")
        val email = "$unique@test.com"
        val rawPassword = "password123!"
        val nickname = unique
        val member = createTestMember(email, rawPassword, nickname)
        val savedMember = memberRepository.saveAndFlush(member)
        val accessToken = loginAndGetAccessToken(email, rawPassword)

        entityManager.createQuery("update Member m set m.status = :status where m.userId = :userId")
            .setParameter("status", MemberStatus.BLACKLISTED)
            .setParameter("userId", savedMember.userId)
            .executeUpdate()
        entityManager.flush()
        entityManager.clear()

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("비로그인 사용자는 내 정보를 조회할 수 없다")
    fun meFailWhenAccessTokenMissing() {
        mvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_MISSING"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("회원 탈퇴 후 기존 accessToken으로 내 정보를 조회할 수 없다")
    fun withdrawThenMeFailsWithExistingAccessToken() {
        val unique = uniqueValue("withdraw-auth")
        val email = "$unique@test.com"
        val rawPassword = "password123!"
        val nickname = unique
        val member = createTestMember(email, rawPassword, nickname)
        val savedMember = memberRepository.saveAndFlush(member)
        val accessToken = loginAndGetAccessToken(email, rawPassword)

        mvc.perform(
            delete("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(savedMember.userId))

        entityManager.flush()
        entityManager.clear()

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("만료된 JWT로 내 정보를 조회할 수 없다")
    fun meFailWhenAccessTokenExpired() {
        val unique = uniqueValue("expired-token")
        val member = createTestMember(
            email = "$unique@test.com",
            rawPassword = "password123!",
            nickname = unique,
        )
        val savedMember = memberRepository.saveAndFlush(member)
        val expiredToken = createExpiredAccessToken(savedMember)

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredToken"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_EXPIRED"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("변조된 JWT로 내 정보를 조회할 수 없다")
    fun meFailWhenAccessTokenTampered() {
        val unique = uniqueValue("tampered-token")
        val member = createTestMember(
            email = "$unique@test.com",
            rawPassword = "password123!",
            nickname = unique,
        )
        val savedMember = memberRepository.saveAndFlush(member)
        val validToken = jwtProvider.createAccessToken(savedMember)
        val tamperedToken = tamperSignature(validToken)

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $tamperedToken"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("Authorization 헤더 형식이 잘못되면 내 정보를 조회할 수 없다")
    fun meFailWhenAuthorizationHeaderInvalid() {
        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer "),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_MISSING"))
            .andExpect(jsonPath("$.timestamp").exists())

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Token invalid-token"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
            .andExpect(jsonPath("$.timestamp").exists())

        mvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "invalid-token"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("로그아웃하면 access_token 쿠키가 정확히 만료된다")
    fun logoutExpiresAccessTokenCookieStrictly() {
        mvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")),
            )
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")),
            )
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")),
            )
            .andExpect(
                header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=")),
            )
    }

    @Test
    @DisplayName("회원 탈퇴 후 기존 이메일과 닉네임으로 다시 가입할 수 있다")
    fun signUpSuccessAfterWithdrawWithSameEmailAndNickname() {
        val unique = uniqueValue("resignup-after-withdraw")
        val email = "$unique@test.com"
        val password = "password123!"
        val nickname = unique
        val member = createTestMember(email, password, nickname)
        val savedMember = memberRepository.saveAndFlush(member)
        val accessToken = jwtProvider.createAccessToken(savedMember)

        mvc.perform(
            delete("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userId").value(savedMember.userId))

        entityManager.flush()
        entityManager.clear()

        mvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password",
                      "nickname": "$nickname"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(handler().handlerType(AuthController::class.java))
            .andExpect(handler().methodName("signUp"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("AUTH_201_SIGNUP_SUCCESS"))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
    }

    private fun loginAndGetAccessToken(
        email: String,
        rawPassword: String,
    ): String {
        val loginResponseBody = mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$rawPassword"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return JsonPath.read(loginResponseBody, "$.data.accessToken")
    }

    private fun createTestMember(
        email: String,
        rawPassword: String,
        nickname: String,
    ): Member {
        return Member.createLocalMember(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            nickname = nickname,
        )
    }

    private fun tamperSignature(token: String): String {
        val lastDotIndex = token.lastIndexOf('.')
        require(lastDotIndex >= 0 && lastDotIndex != token.length - 1) { "Invalid JWT format" }

        val signatureStartIndex = lastDotIndex + 1
        val originalCharacter = token[signatureStartIndex]
        val replacementCharacter = if (originalCharacter == 'x') 'y' else 'x'

        return token.substring(0, signatureStartIndex) +
            replacementCharacter +
            token.substring(signatureStartIndex + 1)
    }

    private fun createExpiredAccessToken(member: Member): String {
        val issuedAt = Instant.now().minusSeconds(7200)
        val expiredAt = Instant.now().minusSeconds(3600)
        val secretKey = Keys.hmacShaKeyFor(
            jwtSecretKey.toByteArray(StandardCharsets.UTF_8),
        )

        return Jwts.builder()
            .subject(member.userId.toString())
            .claim("tokenType", "ACCESS")
            .claim("email", member.email)
            .claim("role", member.role.name)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiredAt))
            .signWith(secretKey)
            .compact()
    }
    private fun uniqueValue(prefix: String): String {
        return "$prefix-${UUID.randomUUID().toString().take(8)}"
    }
}