package com.back.devc.domain.post.comment.attachment.controller;

import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentDeleteResponse;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentUploadRequest;
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService;
import com.back.devc.global.security.jwt.JwtPrincipal;
import com.back.devc.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(CommentAttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentAttachmentService commentAttachmentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("댓글 첨부 업로드 API 호출 성공")
    void uploadCommentAttachments_success() throws Exception {
        CommentAttachmentListResponse response = org.mockito.Mockito.mock(CommentAttachmentListResponse.class);

        given(commentAttachmentService.uploadAttachments(eq(1L), any(CommentAttachmentUploadRequest.class)))
                .willReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.jpg",
                "image/jpeg",
                "dummy-image".getBytes()
        );

        SecurityContextHolder.getContext().setAuthentication(createAuthentication());
        try {
            mockMvc.perform(multipart("/api/comments/{commentId}/attachments", 1L)
                            .file(file)
                            .param("fileOrders", "1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("COMMENT_ATTACHMENT_201_UPLOAD"))
                    .andExpect(jsonPath("$.message").value("댓글 첨부파일 업로드 성공"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(commentAttachmentService).uploadAttachments(eq(1L), any(CommentAttachmentUploadRequest.class));
    }

    @Test
    @DisplayName("댓글 첨부 목록 조회 API 호출 성공")
    void getCommentAttachments_success() throws Exception {
        CommentAttachmentListResponse response = org.mockito.Mockito.mock(CommentAttachmentListResponse.class);

        given(commentAttachmentService.getAttachments(1L)).willReturn(response);

        mockMvc.perform(get("/api/comments/{commentId}/attachments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMENT_ATTACHMENT_200_LIST"))
                .andExpect(jsonPath("$.message").value("댓글 첨부파일 조회 성공"));

        verify(commentAttachmentService).getAttachments(1L);
    }

    @Test
    @DisplayName("댓글 첨부 삭제 API 호출 성공")
    void deleteCommentAttachment_success() throws Exception {
        CommentAttachmentDeleteResponse response = org.mockito.Mockito.mock(CommentAttachmentDeleteResponse.class);

        given(commentAttachmentService.deleteAttachment(1L, 1L)).willReturn(response);

        SecurityContextHolder.getContext().setAuthentication(createAuthentication());
        try {
            mockMvc.perform(delete("/api/comments/{commentId}/attachments/{attachmentId}", 1L, 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMMENT_ATTACHMENT_200_DELETE"))
                    .andExpect(jsonPath("$.message").value("댓글 첨부파일 삭제 성공"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(commentAttachmentService).deleteAttachment(1L, 1L);
    }

    private Authentication createAuthentication() {
        JwtPrincipal principal = new JwtPrincipal(2L, "test@test.com", "USER");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}