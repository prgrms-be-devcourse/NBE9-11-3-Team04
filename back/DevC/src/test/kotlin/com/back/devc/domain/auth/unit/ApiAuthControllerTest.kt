package com.back.devc.domain.auth.unit

import com.back.devc.toNullable
import com.back.devc.domain.auth.controller.AuthController
import com.back.devc.domain.member.member.controller.MemberController
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
internal class ApiAuthControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun 회원가입() {
        val email = "new-user@test.com"
        val password = "password123!"
        val nickname = "newUser"

        val resultActions = mvc.perform(
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
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuthController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("signUp"))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_201_SIGNUP_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").isNumber)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))

        val savedMember = memberRepository.findByEmail(email).toNullable()
            ?: throw AssertionError("Expected saved member")

        Assertions.assertThat(savedMember.nickname).isEqualTo(nickname)
        Assertions.assertThat(savedMember.passwordHash).isNotEqualTo(password)
        Assertions.assertThat(passwordEncoder.matches(password, savedMember.passwordHash)).isTrue()
    }

    @Test
    fun 로그인() {
        val email = "login-user@test.com"
        val rawPassword = "password123!"
        val nickname = "loginUser"

        val member = Member.createLocalMember(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            nickname = nickname,
        )
        memberRepository.save(member)

        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
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
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuthController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_200_LOGIN_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").isNumber)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty)
    }

    @Test
    fun 로그아웃() {
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuthController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("logout"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_200_LOGOUT_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.message").isNotEmpty)
    }

    @Test
    fun 비로그인_상태_확인() {
        mvc.perform(MockMvcRequestBuilders.get("/api/users/me"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_TOKEN_MISSING"))
    }

    @Test
    fun 내정보_조회() {
        val email = "me-user@test.com"
        val rawPassword = "password123!"
        val nickname = "meUser"

        val member = Member.createLocalMember(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            nickname = nickname,
        )
        memberRepository.save(member)

        val loginResponse = mvc.perform(
            MockMvcRequestBuilders.post("/api/auth/login")
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
            .andReturn()
            .response
            .contentAsString

        val token = JsonPath.read<String>(loginResponse, "$.data.accessToken")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header("Authorization", "Bearer $token"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("USER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun 동시에_같은_이메일과_닉네임으로_회원가입하면_하나만_성공한다() {
        val email = "concurrent-signup@test.com"
        val password = "password123!"
        val nickname = "concurrentUser"
        val executorService = Executors.newFixedThreadPool(2)

        try {
            val firstRequest = CompletableFuture.supplyAsync(
                { performSignUp(email, password, nickname) },
                executorService,
            )
            val secondRequest = CompletableFuture.supplyAsync(
                { performSignUp(email, password, nickname) },
                executorService,
            )

            val statuses = listOf(firstRequest.join(), secondRequest.join())

            Assertions.assertThat(statuses).contains(201)
            Assertions.assertThat(statuses).contains(409)

            val savedMemberCount = memberRepository.findAll()
                .count { member -> email == member.email }

            Assertions.assertThat(savedMemberCount).isEqualTo(1)
        } finally {
            memberRepository.findAll()
                .filter { member -> email == member.email || nickname == member.nickname }
                .forEach { memberRepository.delete(it) }

            executorService.shutdown()
        }
    }

    private fun performSignUp(
        email: String,
        password: String,
        nickname: String,
    ): Int {
        try {
            return mvc.perform(
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
                .andReturn()
                .response
                .status
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
