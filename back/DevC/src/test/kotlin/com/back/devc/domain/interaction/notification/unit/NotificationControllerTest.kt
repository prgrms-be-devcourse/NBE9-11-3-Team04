package com.back.devc.domain.interaction.notification.unit

import com.back.devc.domain.interaction.notification.controller.NotificationController
import com.back.devc.domain.interaction.notification.dto.NotificationListResponse
import com.back.devc.domain.interaction.notification.dto.NotificationResponse
import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime

@ActiveProfiles("test")
@WebMvcTest(NotificationController::class)
@AutoConfigureMockMvc(addFilters = false)
internal class NotificationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var notificationService: NotificationService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var memberRepository: MemberRepository

    @MockitoBean
    private lateinit var jpaMetamodelMappingContext: JpaMetamodelMappingContext

    @Test
    @DisplayName("내 알림 목록 조회 API 호출 성공")
    fun getMyNotificationsSuccess() {
        val notification = NotificationResponse(
            notificationId = 1L,
            userId = 1L,
            actorUserId = 2L,
            actorNickname = "작성자B",
            postId = 100L,
            commentId = 200L,
            type = "COMMENT",
            message = "작성자B님이 게시글에 댓글을 남겼습니다.",
            isRead = false,
            createdAt = LocalDateTime.now(),
        )
        val response = NotificationListResponse(
            notifications = listOf(notification),
            page = 0,
            size = 20,
            totalElements = 1,
            totalPages = 1,
            hasNext = false,
        )

        BDDMockito.given(notificationService.getMyNotifications(1L, 0, 20, "all"))
            .willReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/notifications")
                .param("page", "0")
                .param("size", "20")
                .param("tab", "all")
                .principal(createAuthentication()),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("NOTIFICATION_200_LIST"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("알림 목록 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.notifications.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.size").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalElements").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalPages").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.hasNext").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.notifications[0].type").value("COMMENT"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.notifications[0].message")
                    .value("작성자B님이 게시글에 댓글을 남겼습니다."),
            )

        Mockito.verify(notificationService).getMyNotifications(1L, 0, 20, "all")
    }

    @Test
    @DisplayName("알림 읽음 처리 API 호출 성공")
    fun readNotificationSuccess() {
        val response = NotificationResponse(
            notificationId = 1L,
            userId = 1L,
            actorUserId = 2L,
            actorNickname = "작성자B",
            postId = 100L,
            commentId = 200L,
            type = "COMMENT",
            message = "작성자B님이 게시글에 댓글을 남겼습니다.",
            isRead = true,
            createdAt = LocalDateTime.now(),
        )

        BDDMockito.given(notificationService.readNotification(1L, 1L))
            .willReturn(response)

        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/notifications/{notificationId}/read", 1L)
                .principal(createAuthentication()),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("NOTIFICATION_200_READ"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("알림 읽음 처리 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.notificationId").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.isRead").value(true))

        Mockito.verify(notificationService).readNotification(1L, 1L)
    }

    private fun createAuthentication(): Authentication {
        val principal = JwtPrincipal(1L, "test@test.com", "USER")
        val authorities = emptyList<GrantedAuthority>()

        return UsernamePasswordAuthenticationToken(principal, null, authorities)
    }
}