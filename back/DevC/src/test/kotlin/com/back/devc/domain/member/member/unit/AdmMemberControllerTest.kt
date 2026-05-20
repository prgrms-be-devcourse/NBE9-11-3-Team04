package com.back.devc.domain.member.member.unit

import com.back.devc.domain.member.member.controller.AdmMemberController
import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse
import com.back.devc.domain.member.member.dto.AdmMemberListRequest
import com.back.devc.domain.member.member.dto.AdmMemberListResponse
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.member.service.AdmMemberService
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.response.successCode.MemberSuccessCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@DisplayName("AdmMemberController 테스트")
internal class AdmMemberControllerTest {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val admMemberService = object : AdmMemberService(
            Mockito.mock(MemberRepository::class.java),
            Mockito.mock(PostRepository::class.java),
            Mockito.mock(CommentRepository::class.java),
        ) {
            public override fun getMembers(request: AdmMemberListRequest): Page<AdmMemberListResponse> {
                val dto = AdmMemberListResponse(
                    userId = 1L,
                    email = "test@test.com",
                    nickname = "nick",
                    postCount = 10,
                    commentCount = 5,
                    status = MemberStatus.ACTIVE,
                    createdAt = LocalDateTime.now(),
                    suspendedUntil = null,
                )

                return PageImpl(listOf(dto), PageRequest.of(0, 20), 1)
            }

            public override fun getMemberDetail(userId: Long): AdmMemberDetailResponse {
                return AdmMemberDetailResponse(
                    userId = userId,
                    email = "test@test.com",
                    nickname = "nick",
                    postCount = 0L,
                    commentCount = 0L,
                    status = MemberStatus.ACTIVE,
                    createdAt = LocalDateTime.now(),
                    suspendedUntil = null,
                )
            }

            public override fun updateMemberStatus(
                userId: Long,
                request: AdmMemberStatusUpdateRequest,
            ): AdmMemberDetailResponse {
                return AdmMemberDetailResponse(
                    userId = userId,
                    email = "test@test.com",
                    nickname = "nick",
                    postCount = 0L,
                    commentCount = 0L,
                    status = requireNotNull(request.status),
                    createdAt = LocalDateTime.now(),
                    suspendedUntil = null,
                )
            }
        }

        mockMvc = MockMvcBuilders
            .standaloneSetup(AdmMemberController(admMemberService))
            .build()
    }

    @Test
    @DisplayName("회원 목록 조회 성공")
    fun getMembersSuccess() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/admin/members")
                .param("page", "0")
                .param("size", "20"),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(MemberSuccessCode.ADMIN_MEMBER_LIST_SUCCESS.code))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].userId").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].nickname").value("nick"))
    }

    @Test
    @DisplayName("회원 상세 조회 성공")
    fun getMemberDetailSuccess() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/members/1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(MemberSuccessCode.ADMIN_MEMBER_DETAIL_SUCCESS.code))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(1L))
    }

    @Test
    @DisplayName("회원 상태 변경 성공")
    fun updateMemberStatusSuccess() {
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/admin/members/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "status": "ACTIVE",
                      "days": null
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(MemberSuccessCode.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS.code))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.userId").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACTIVE"))
    }
}