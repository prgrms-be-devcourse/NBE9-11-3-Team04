package com.back.devc.domain.member.member

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberRole
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.global.security.jwt.JwtProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdmMember integration")
class AdmMemberIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Test
    @DisplayName("admin lists and filters members without changing response shape")
    fun adminListsMembers() {
        val token = createAdminToken("admin-list-token@test.com")
        memberRepository.saveAndFlush(Member.createLocalMember("adm-list@test.com", "password", "adm-list"))

        mockMvc.perform(
            get("/api/admin/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .param("page", "0")
                .param("size", "20")
                .param("keyword", "adm-list")
                .param("status", "ACTIVE"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("ADMIN_MEMBER_200_LIST_SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].email").value("adm-list@test.com"))
            .andExpect(jsonPath("$.data.content[0].nickname").value("adm-list"))
            .andExpect(jsonPath("$.data.content[0].postCount").value(0))
            .andExpect(jsonPath("$.data.content[0].commentCount").value(0))
    }

    @Test
    @DisplayName("admin reads member detail and updates status")
    fun adminReadsDetailAndUpdatesStatus() {
        val token = createAdminToken("adm-detail-admin@test.com")
        val member = memberRepository.saveAndFlush(
            Member.createLocalMember("adm-detail@test.com", "password", "adm-detail"),
        )
        val userId = requireNotNull(member.userId)

        mockMvc.perform(
            get("/api/admin/members/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("ADMIN_MEMBER_200_DETAIL_SUCCESS"))
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))

        mockMvc.perform(
            patch("/api/admin/members/{userId}/status", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "WARNED",
                      "days": null
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("ADMIN_MEMBER_200_STATUS_UPDATE_SUCCESS"))
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.status").value("WARNED"))

        val updated = memberRepository.findById(userId).orElseThrow()
        assert(updated.status == MemberStatus.WARNED)
    }

    private fun createAdminToken(email: String): String {
        val admin = memberRepository.saveAndFlush(
            Member.createLocalMember(email, "password", email.substringBefore("@")).also {
                it.updateRole(MemberRole.ADMIN)
            },
        )

        return jwtProvider.createAccessToken(admin)
    }
}
