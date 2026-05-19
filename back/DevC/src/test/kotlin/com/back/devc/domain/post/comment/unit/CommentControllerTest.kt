package com.back.devc.domain.post.comment.unit

import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.controller.CommentController
import com.back.devc.domain.post.comment.dto.CommentDeleteResponse
import com.back.devc.domain.post.comment.dto.CommentListResponse
import com.back.devc.domain.post.comment.dto.CommentResponse
import com.back.devc.domain.post.comment.service.CommentService
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
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@WebMvcTest(CommentController::class)
@AutoConfigureMockMvc(addFilters = false)
internal class CommentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var commentService: CommentService

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
    @DisplayName("댓글 작성 API 호출 성공")
    fun createCommentSuccess() {
        val response = mock(CommentResponse::class.java)

        given(
            commentService.createComment(
                eq(1L),
                eq(2L),
                anyNotNull(),
            ),
        ).willReturn(response)

        SecurityContextHolder.getContext().authentication = createAuthentication()

        mockMvc.perform(
            post("/api/posts/{postId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "첫번째 댓글"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("COMMENT_201_CREATE"))
            .andExpect(jsonPath("$.message").value("댓글 작성 성공"))

        verify(commentService).createComment(
            eq(1L),
            eq(2L),
            anyNotNull(),
        )
    }

    @Test
    @DisplayName("대댓글 작성 API 호출 성공")
    fun createReplySuccess() {
        val response = mock(CommentResponse::class.java)

        given(
            commentService.createReply(
                eq(1L),
                eq(2L),
                anyNotNull(),
            ),
        ).willReturn(response)

        SecurityContextHolder.getContext().authentication = createAuthentication()

        mockMvc.perform(
            post("/api/comments/{commentId}/replies", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "대댓글입니다."
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("COMMENT_201_REPLY"))
            .andExpect(jsonPath("$.message").value("대댓글 작성 성공"))

        verify(commentService).createReply(
            eq(1L),
            eq(2L),
            anyNotNull(),
        )
    }

    @Test
    @DisplayName("댓글 수정 API 호출 성공")
    fun updateCommentSuccess() {
        val response = mock(CommentResponse::class.java)

        given(
            commentService.updateComment(
                eq(1L),
                eq(2L),
                anyNotNull(),
            ),
        ).willReturn(response)

        SecurityContextHolder.getContext().authentication = createAuthentication()

        mockMvc.perform(
            patch("/api/comments/{commentId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "content": "수정된 댓글"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMENT_200_UPDATE"))
            .andExpect(jsonPath("$.message").value("댓글 수정 성공"))

        verify(commentService).updateComment(
            eq(1L),
            eq(2L),
            anyNotNull(),
        )
    }

    @Test
    @DisplayName("댓글 삭제 API 호출 성공")
    fun deleteCommentSuccess() {
        val response = mock(CommentDeleteResponse::class.java)

        given(commentService.deleteComment(1L, 2L))
            .willReturn(response)

        SecurityContextHolder.getContext().authentication = createAuthentication()

        mockMvc.perform(delete("/api/comments/{commentId}", 1L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMENT_200_DELETE"))
            .andExpect(jsonPath("$.message").value("댓글 삭제 성공"))

        verify(commentService).deleteComment(1L, 2L)
    }

    @Test
    @DisplayName("게시글 댓글 목록 조회 API 호출 성공")
    fun getCommentsSuccess() {
        val response = mock(CommentListResponse::class.java)

        given(commentService.getComments(1L, 0, 20))
            .willReturn(response)

        mockMvc.perform(
            get("/api/posts/{postId}/comments", 1L)
                .param("page", "0")
                .param("size", "20"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("COMMENT_200_LIST"))
            .andExpect(jsonPath("$.message").value("댓글 목록 조회 성공"))

        verify(commentService).getComments(1L, 0, 20)
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
