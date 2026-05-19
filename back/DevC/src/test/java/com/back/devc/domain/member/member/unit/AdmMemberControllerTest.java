package com.back.devc.domain.member.member.unit;

import com.back.devc.domain.member.member.controller.AdmMemberController;
import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse;
import com.back.devc.domain.member.member.dto.AdmMemberListRequest;
import com.back.devc.domain.member.member.dto.AdmMemberListResponse;
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.member.service.AdmMemberService;
import com.back.devc.global.response.successCode.MemberSuccessCode;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

@DisplayName("AdmMemberController 테스트")
class AdmMemberControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdmMemberService admMemberService = new AdmMemberService(
                mock(MemberRepository.class),
                mock(PostRepository.class),
                mock(CommentRepository.class)
        ) {
            @Override
            public Page<AdmMemberListResponse> getMembers(AdmMemberListRequest request) {
                AdmMemberListResponse dto =
                        new AdmMemberListResponse(
                                1L,
                                "test@test.com",
                                "nick",
                                10,
                                5,
                                MemberStatus.ACTIVE,
                                LocalDateTime.now(),
                                LocalDateTime.now()
                        );

                return new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
            }

            @Override
            public AdmMemberDetailResponse getMemberDetail(long userId) {
                return new AdmMemberDetailResponse(
                        userId,
                        "test@test.com",
                        "nick",
                        0L,
                        0L,
                        MemberStatus.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
            }

            @Override
            public AdmMemberDetailResponse updateMemberStatus(
                    long userId,
                    AdmMemberStatusUpdateRequest request
            ) {
                return new AdmMemberDetailResponse(
                        userId,
                        "test@test.com",
                        "nick",
                        0L,
                        0L,
                        request.getStatus(),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
            }
        };

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdmMemberController(admMemberService))
                .build();
    }

    @Test
    @DisplayName("회원 목록 조회 성공")
    void getMembers_success() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MemberSuccessCode.ADMIN_MEMBER_LIST_SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].userId").value(1L))
                .andExpect(jsonPath("$.data.content[0].nickname").value("nick"));
    }

    @Test
    @DisplayName("회원 상세 조회 성공")
    void getMemberDetail_success() throws Exception {
        mockMvc.perform(get("/api/admin/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MemberSuccessCode.ADMIN_MEMBER_DETAIL_SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(1L));
    }

    @Test
    @DisplayName("회원 상태 변경 성공")
    void updateMemberStatus_success() throws Exception {
        mockMvc.perform(patch("/api/admin/members/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACTIVE",
                                  "days": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MemberSuccessCode.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
