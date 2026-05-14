package com.back.devc.domain.interaction.notification.controller;

import com.back.devc.domain.interaction.notification.dto.NotificationListResponse;
import com.back.devc.domain.interaction.notification.dto.NotificationResponse;
import com.back.devc.domain.interaction.notification.service.NotificationService;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.global.security.jwt.JwtPrincipal;
import com.back.devc.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("내 알림 목록 조회 API 호출 성공")
    void getMyNotifications_success() throws Exception {
        NotificationResponse notification = new NotificationResponse(
                1L,
                1L,
                2L,
                "작성자B",
                100L,
                200L,
                "COMMENT",
                "작성자B님이 게시글에 댓글을 남겼습니다.",
                false,
                null
        );
        NotificationListResponse response = new NotificationListResponse(
                List.of(notification),
                0,
                20,
                1,
                1,
                false
        );

        given(notificationService.getMyNotifications(1L, 0, 20, "all")).willReturn(response);
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20")
                        .param("tab", "all")
                        .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200_LIST"))
                .andExpect(jsonPath("$.message").value("알림 목록 조회 성공"))
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.notifications[0].type").value("COMMENT"))
                .andExpect(jsonPath("$.data.notifications[0].message").value("작성자B님이 게시글에 댓글을 남겼습니다."));
        verify(notificationService).getMyNotifications(1L, 0, 20, "all");
    }

    @Test
    @DisplayName("알림 읽음 처리 API 호출 성공")
    void readNotification_success() throws Exception {
        NotificationResponse response = new NotificationResponse(
                1L,
                1L,
                2L,
                "작성자B",
                100L,
                200L,
                "COMMENT",
                "작성자B님이 게시글에 댓글을 남겼습니다.",
                true,
                null
        );

        given(notificationService.readNotification(1L, 1L)).willReturn(response);
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", 1L)
                        .principal(createAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_200_READ"))
                .andExpect(jsonPath("$.message").value("알림 읽음 처리 성공"))
                .andExpect(jsonPath("$.data.notificationId").value(1))
                .andExpect(jsonPath("$.data.isRead").value(true));
        verify(notificationService).readNotification(1L, 1L);
    }

    private Authentication createAuthentication() {
        JwtPrincipal principal = new JwtPrincipal(1L, "test@test.com", "USER");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
