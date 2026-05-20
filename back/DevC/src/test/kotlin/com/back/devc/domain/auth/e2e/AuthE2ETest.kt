package com.back.devc.domain.auth.e2e

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.collections.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("인증 E2E 테스트")
internal class AuthE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        // E2E 테스트가 기존 테스트 데이터에 의존하지 않도록 관련 테이블을 초기화한다.
        jdbcTemplate.update("DELETE FROM NOTIFICATIONS")
        jdbcTemplate.update("DELETE FROM COMMENT_ATTACHMENTS")
        jdbcTemplate.update("DELETE FROM COMMENTS")
        jdbcTemplate.update("DELETE FROM POST_LIKES")
        jdbcTemplate.update("DELETE FROM BOOKMARKS")
        jdbcTemplate.update("DELETE FROM REPORTS")
        jdbcTemplate.update("DELETE FROM SEARCH_LOGS")
        jdbcTemplate.update("DELETE FROM POST")
        jdbcTemplate.update("DELETE FROM USERS")
    }

    @Test
    @DisplayName("회원가입 후 로그인하고 내 정보를 조회한 뒤 로그아웃할 수 있다")
    fun signupThenLoginThenGetMeThenLogout() {
        val email = "auth-flow@test.com"
        val password = "password123!"
        val nickname = "authFlowUser"

        val signupResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
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
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_201_SIGNUP_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))
            .andReturn()

        val userId = extractLong(signupResult, "userId")

        val savedMember = memberRepository.findByEmail(email).orElseThrow()

        Assertions.assertThat(savedMember.userId).isEqualTo(userId)
        Assertions.assertThat(savedMember.passwordHash).isNotEqualTo(password)
        Assertions.assertThat(passwordEncoder.matches(password, savedMember.passwordHash)).isTrue()

        val loginResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
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
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("access_token=")))
            .andExpect(
                MockMvcResultMatchers.header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("HttpOnly")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_200_LOGIN_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(userId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty)
            .andReturn()

        val accessToken = extractString(loginResult, "accessToken")

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(userId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("access_token=")))
            .andExpect(
                MockMvcResultMatchers.header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.message").isNotEmpty)
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 실패한다")
    fun signupFailWhenEmailAlreadyExists() {
        val email = "duplicate-email@test.com"
        val password = "password123!"

        saveLocalMember(email, password, "duplicateEmailUser", MemberStatus.ACTIVE)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
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
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_409_EMAIL"))

        val count = memberRepository.findAll()
            .count { member -> email == member.email }

        Assertions.assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("이미 가입된 닉네임으로 회원가입하면 실패한다")
    fun signupFailWhenNicknameAlreadyExists() {
        val nickname = "duplicateNicknameUser"
        val password = "password123!"

        saveLocalMember("duplicate-nickname@test.com", password, nickname, MemberStatus.ACTIVE)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "new-email-for-nickname@test.com",
                      "password": "$password",
                      "nickname": "$nickname"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_409_NICKNAME"))

        val count = memberRepository.findAll()
            .count { member -> nickname == member.nickname }

        Assertions.assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("잘못된 회원가입 요청값이면 실패한다")
    fun signupFailWhenRequestInvalid() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
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
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_400"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.validation").exists())
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 실패한다")
    fun loginFailWhenEmailNotFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
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
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_404_EMAIL_NOT_FOUND"))
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    fun loginFailWhenPasswordMismatch() {
        val email = "password-mismatch@test.com"
        val password = "password123!"

        saveLocalMember(email, password, "passwordMismatchUser", MemberStatus.ACTIVE)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
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
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_PASSWORD_MISMATCH"))
    }

    @Test
    @DisplayName("블랙리스트 회원은 로그인할 수 없다")
    fun loginFailWhenMemberBlacklisted() {
        val email = "blacklisted-auth@test.com"
        val password = "password123!"

        saveLocalMember(email, password, "blacklistedAuthUser", MemberStatus.BLACKLISTED)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
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
            .andExpect(MockMvcResultMatchers.status().isForbidden)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_403_MEMBER_BLACKLISTED"))
    }

    @Test
    @DisplayName("비로그인 사용자는 내 정보를 조회할 수 없다")
    fun meFailWhenAccessTokenMissing() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/me"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_TOKEN_MISSING"))
    }

    @Test
    @DisplayName("삭제된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    fun meFailWhenMemberDeletedAfterLogin() {
        val email = "deleted-after-login@test.com"
        val password = "password123!"

        val member = saveLocalMember(email, password, "deletedAfterLoginUser", MemberStatus.ACTIVE)
        val accessToken = loginAndExtractAccessToken(email, password)

        memberRepository.deleteById(requireNotNull(member.userId))
        memberRepository.flush()

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
    }

    @Test
    @DisplayName("블랙리스트 처리된 회원은 기존 accessToken으로 내 정보를 조회할 수 없다")
    fun meFailWhenMemberBlacklistedAfterLogin() {
        val email = "blacklisted-after-login@test.com"
        val password = "password123!"

        val member = saveLocalMember(email, password, "blacklistedAfterLoginUser", MemberStatus.ACTIVE)
        val accessToken = loginAndExtractAccessToken(email, password)

        member.updateStatus(MemberStatus.BLACKLISTED)
        memberRepository.saveAndFlush(member)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
    }

    @Test
    @DisplayName("회원 탈퇴 후 기존 accessToken으로 내 정보를 조회할 수 없다")
    fun withdrawThenMeFailsWithExistingAccessToken() {
        val email = "withdraw-auth@test.com"
        val password = "password123!"

        val member = saveLocalMember(email, password, "withdrawAuthUser", MemberStatus.ACTIVE)
        val accessToken = loginAndExtractAccessToken(email, password)

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(member.userId))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
    }

    private fun saveLocalMember(
        email: String,
        rawPassword: String,
        nickname: String,
        status: MemberStatus,
    ): Member {
        val member = Member.createLocalMember(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            nickname = nickname,
        )
        member.updateStatus(status)

        return memberRepository.saveAndFlush(member)
    }

    private fun loginAndExtractAccessToken(
        email: String,
        password: String,
    ): String {
        val loginResult = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
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
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty)
            .andReturn()

        return extractString(loginResult, "accessToken")
    }

    private fun extractLong(
        result: MvcResult,
        fieldName: String,
    ): Long {
        val responseBody = readResponseBody(result)
        val data = responseBody["data"] as Map<*, *>

        return toLong(data[fieldName])
    }

    private fun extractString(
        result: MvcResult,
        fieldName: String,
    ): String {
        val responseBody = readResponseBody(result)
        val data = responseBody["data"] as Map<*, *>

        return data[fieldName].toString()
    }

    private fun readResponseBody(result: MvcResult): Map<*, *> {
        return objectMapper.readValue(
            result.response.contentAsString,
            Map::class.java,
        )
    }

    private fun toLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            else -> value.toString().toLong()
        }
    }
}