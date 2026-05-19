package com.back.devc.domain.member.member.e2e

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberRole
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.global.security.jwt.JwtProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdmMember E2E")
class AdmMemberE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Test
    @DisplayName("admin token can call admin member APIs")
    fun adminTokenCanCallAdminMemberApis() {
        val admin = memberRepository.saveAndFlush(
            Member.createLocalMember("adm-e2e-admin@test.com", "password", "adm-e2e-admin").also {
                it.updateRole(MemberRole.ADMIN)
            },
        )
        memberRepository.saveAndFlush(Member.createLocalMember("adm-e2e-user@test.com", "password", "adm-e2e-user"))
        val token = jwtProvider.createAccessToken(admin)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .param("page", "0")
                .param("size", "20")
                .param("keyword", "adm-e2e-user"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("ADMIN_MEMBER_200_LIST_SUCCESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].email").value("adm-e2e-user@test.com"))
    }

    @Test
    @DisplayName("user token cannot call admin member APIs")
    fun userTokenCannotCallAdminMemberApis() {
        val user = memberRepository.saveAndFlush(Member.createLocalMember("adm-e2e-denied@test.com", "password", "denied"))
        val token = jwtProvider.createAccessToken(user)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .param("page", "0")
                .param("size", "20"),
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }
}