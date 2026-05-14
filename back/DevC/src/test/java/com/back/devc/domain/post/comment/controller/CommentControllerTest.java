package com.back.devc.domain.post.comment.controller;

import com.back.devc.domain.post.comment.dto.CommentDeleteResponse;
import com.back.devc.domain.post.comment.dto.CommentListResponse;
import com.back.devc.domain.post.comment.dto.CommentResponse;
import com.back.devc.domain.post.comment.service.CommentService;
import com.back.devc.global.security.jwt.JwtProvider;
import com.back.devc.domain.member.member.repository.MemberRepository;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.back.devc.global.security.jwt.JwtPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("test")
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("댓글 작성 API 호출 성공")
    void createComment_success() throws Exception {
        CommentResponse response = org.mockito.Mockito.mock(CommentResponse.class);

        given(commentService.createComment(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.any()
        )).willReturn(response);

        SecurityContextHolder.getContext().setAuthentication(createAuthentication());
        try {
            mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "첫번째 댓글"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("COMMENT_201_CREATE"))
                    .andExpect(jsonPath("$.message").value("댓글 작성 성공"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(commentService).createComment(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("대댓글 작성 API 호출 성공")
    void createReply_success() throws Exception {
        CommentResponse response = org.mockito.Mockito.mock(CommentResponse.class);

        given(commentService.createReply(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.any()
        )).willReturn(response);

        SecurityContextHolder.getContext().setAuthentication(createAuthentication());
        try {
            mockMvc.perform(post("/api/comments/{commentId}/replies", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "대댓글입니다."
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("COMMENT_201_REPLY"))
                    .andExpect(jsonPath("$.message").value("대댓글 작성 성공"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(commentService).createReply(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("댓글 수정 API 호출 성공")
    void updateComment_success() throws Exception {
        CommentResponse response = org.mockito.Mockito.mock(CommentResponse.class);

        given(commentService.updateComment(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.any()
        )).willReturn(response);

        SecurityContextHolder.getContext().setAuthentication(createAuthentication());
        try {
            mockMvc.perform(patch("/api/comments/{commentId}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "수정된 댓글"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMENT_200_UPDATE"))
                    .andExpect(jsonPath("$.message").value("댓글 수정 성공"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(commentService).updateComment(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("댓글 삭제 API 호출 성공")
    void deleteComment_success() throws Exception {
        CommentDeleteResponse response = org.mockito.Mockito.mock(CommentDeleteResponse.class);

        given(commentService.deleteComment(1L, 2L)).willReturn(response);

        SecurityContextHolder.getContext().setAuthentication(createAuthentication());
        try {
            mockMvc.perform(delete("/api/comments/{commentId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMENT_200_DELETE"))
                    .andExpect(jsonPath("$.message").value("댓글 삭제 성공"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(commentService).deleteComment(1L, 2L);
    }

    @Test
    @DisplayName("게시글 댓글 목록 조회 API 호출 성공")
    void getComments_success() throws Exception {
        CommentListResponse response = org.mockito.Mockito.mock(CommentListResponse.class);

        given(commentService.getComments(1L)).willReturn(response);

        mockMvc.perform(get("/api/posts/{postId}/comments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMENT_200_LIST"))
                .andExpect(jsonPath("$.message").value("댓글 목록 조회 성공"));

        verify(commentService).getComments(1L);
    }

    private Authentication createAuthentication() {
        JwtPrincipal principal = new JwtPrincipal(2L, "test@test.com", "USER");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
