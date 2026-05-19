package com.back.devc.domain.post.comment.attachment.unit

import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.attachment.controller.CommentAttachmentController
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentDeleteResponse
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@WebMvcTest(CommentAttachmentController::class)
@AutoConfigureMockMvc(addFilters = false)
internal class CommentAttachmentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var commentAttachmentService: CommentAttachmentService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @MockitoBean
    private lateinit var memberRepository: MemberRepository

    @MockitoBean
    private lateinit var jpaMetamodelMappingContext: JpaMetamodelMappingContext

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("댓글 첨부 업로드 API 호출 성공")
    fun uploadCommentAttachmentsSuccess() {
        val response = mock(CommentAttachmentListResponse::class.java)

        given(
            commentAttachmentService.uploadAttachments(
                eq(1L),
                anyNotNull(),
            ),
        ).willReturn(response)

        val file = MockMultipartFile(
            "files",
            "test.jpg",
            "image/jpeg",
            "dummy-image".toByteArray(),
        )

        SecurityContextHolder.getContext().authentication = createAuthentication()

        mockMvc.perform(
            multipart("/api/comments/{commentId}/attachments", 1L)
                .file(file)
                .param("fileOrders", "1"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("COMMENT_ATTACHMENT_201_UPLOAD"))
            .andExpect(jsonPath("$.message").value("댓글 첨부파일 업로드 성공"))

        verify(commentAttachmentService).uploadAttachments(
            eq(1L),
            anyNotNull(),
        )
    }

    @Test
    @DisplayName("댓글 첨부 목록 조회 API 호출 성공")
    fun getCommentAttachmentsSuccess() {
        val response = mock(CommentAttachmentListResponse::class.java)

        given(commentAttachmentService.getAttachments(1L))
            .willReturn(response)

        mockMvc.perform(get("/api/comments/{commentId}/attachments", 1L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMENT_ATTACHMENT_200_LIST"))
            .andExpect(jsonPath("$.message").value("댓글 첨부파일 조회 성공"))

        verify(commentAttachmentService).getAttachments(1L)
    }

    @Test
    @DisplayName("댓글 첨부 삭제 API 호출 성공")
    fun deleteCommentAttachmentSuccess() {
        val response = mock(CommentAttachmentDeleteResponse::class.java)

        given(commentAttachmentService.deleteAttachment(1L, 1L))
            .willReturn(response)

        SecurityContextHolder.getContext().authentication = createAuthentication()

        mockMvc.perform(delete("/api/comments/{commentId}/attachments/{attachmentId}", 1L, 1L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMENT_ATTACHMENT_200_DELETE"))
            .andExpect(jsonPath("$.message").value("댓글 첨부파일 삭제 성공"))

        verify(commentAttachmentService).deleteAttachment(1L, 1L)
    }

    private fun createAuthentication(): Authentication {
        val principal = JwtPrincipal(2L, "test@test.com", "USER")
        val authorities = emptyList<GrantedAuthority>()

        return UsernamePasswordAuthenticationToken(principal, null, authorities)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNotNull(): T {
        any<T>()
        return null as T
    }
}