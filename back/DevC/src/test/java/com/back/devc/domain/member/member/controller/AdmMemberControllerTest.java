package com.back.devc.domain.member.member.controller;

import com.back.devc.domain.member.member.dto.AdmMemberDetailResponse;
import com.back.devc.domain.member.member.dto.AdmMemberListResponse;
import com.back.devc.domain.member.member.dto.AdmMemberStatusUpdateRequest;
import com.back.devc.domain.member.member.entity.MemberStatus;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.member.service.AdmMemberService;
import com.back.devc.global.response.successCode.MemberSuccessCode;
import com.back.devc.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdmMemberController.class)
@ActiveProfiles("test")
@DisplayName("AdmMemberController 테스트")
@AutoConfigureMockMvc(addFilters = false)
class AdmMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdmMemberService admMemberService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("회원 목록 조회 성공")
    void getMembers_success() throws Exception {

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

        Page<AdmMemberListResponse> page =
                new PageImpl<>(List.of(dto));

        given(admMemberService.getMembers(any()))
                .willReturn(page);

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

        AdmMemberDetailResponse dto =
                new AdmMemberDetailResponse(
                        1L,
                        "test@test.com",
                        "nick",
                        0L,
                        0L,
                        MemberStatus.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        given(admMemberService.getMemberDetail(1L))
                .willReturn(dto);

        mockMvc.perform(get("/api/admin/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MemberSuccessCode.ADMIN_MEMBER_DETAIL_SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(1L));
    }

    @Test
    @DisplayName("회원 상태 변경 성공")
    void updateMemberStatus_success() throws Exception {

        AdmMemberStatusUpdateRequest request =
                new AdmMemberStatusUpdateRequest(MemberStatus.ACTIVE);

        AdmMemberDetailResponse response =
                new AdmMemberDetailResponse(
                        1L,
                        "test@test.com",
                        "nick",
                        0L,
                        0L,
                        MemberStatus.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        given(admMemberService.updateMemberStatus(eq(1L), any()))
                .willReturn(response);

        mockMvc.perform(patch("/api/admin/members/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value(MemberSuccessCode.ADMIN_MEMBER_STATUS_UPDATE_SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(1L));
    }
}