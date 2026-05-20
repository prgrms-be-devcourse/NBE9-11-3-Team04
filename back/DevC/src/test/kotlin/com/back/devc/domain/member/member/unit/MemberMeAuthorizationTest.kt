package com.back.devc.domain.member.member.unit

import com.back.devc.domain.member.member.controller.MemberController
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.jayway.jsonpath.JsonPath
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.DisplayName
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
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
internal class MemberMeAuthorizationTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @DisplayName("내정보 조회 - 정상 사용자면 200 응답")
    fun meAuthorizedUserSuccess() {
        val email = "me-ok@test.com"
        val rawPassword = "password123!"
        val nickname = "meOkUser"

        val member = Member.createLocalMember(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            nickname = nickname,
        )
        memberRepository.save(member)

        val accessToken = loginAndGetAccessToken(email, rawPassword)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header("Authorization", "Bearer $accessToken"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_200_ME_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))
    }

    @Test
    @DisplayName("내정보 조회 - 블랙리스트 사용자면 401 응답")
    fun meBlacklistedUserUnauthorized() {
        val email = "me-blacklisted@test.com"
        val rawPassword = "password123!"
        val nickname = "meBlockedUser"

        val member = Member.createLocalMember(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            nickname = nickname,
        )
        val savedMember = memberRepository.save(member)
        val accessToken = loginAndGetAccessToken(email, rawPassword)

        entityManager.createQuery("update Member m set m.status = :status where m.userId = :userId")
            .setParameter("status", MemberStatus.BLACKLISTED)
            .setParameter("userId", savedMember.userId)
            .executeUpdate()
        entityManager.flush()
        entityManager.clear()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header("Authorization", "Bearer $accessToken"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
    }

    @Test
    @DisplayName("내정보 조회 - 삭제된 사용자면 401 응답")
    fun meDeletedUserUnauthorized() {
        val email = "me-deleted@test.com"
        val rawPassword = "password123!"
        val nickname = "meDeletedUser"

        val member = Member.createLocalMember(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
            nickname = nickname,
        )
        val savedMember = memberRepository.save(member)
        val accessToken = loginAndGetAccessToken(email, rawPassword)

        memberRepository.deleteById(requireNotNull(savedMember.userId))
        memberRepository.flush()
        entityManager.clear()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/users/me")
                .header("Authorization", "Bearer $accessToken"),
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("AUTH_401_TOKEN_INVALID"))
    }

    private fun loginAndGetAccessToken(
        email: String,
        rawPassword: String,
    ): String {
        val responseBody = mvc.perform(
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
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        return JsonPath.read(responseBody, "$.data.accessToken")
    }
}