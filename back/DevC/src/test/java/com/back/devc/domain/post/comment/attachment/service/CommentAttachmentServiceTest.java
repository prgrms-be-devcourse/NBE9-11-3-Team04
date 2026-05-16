package com.back.devc.domain.post.comment.attachment.service;

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse;
import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment;
import com.back.devc.domain.post.comment.attachment.repository.CommentAttachmentRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.global.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentAttachmentServiceTest {

    @Mock
    private CommentAttachmentRepository commentAttachmentRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentAttachmentService commentAttachmentService;

    @Test
    @DisplayName("댓글 첨부 목록 조회 성공")
    void getAttachments_success() {
        given(commentRepository.findById(1L)).willReturn(Optional.of(mock(Comment.class)));

        CommentAttachment attachment1 = CommentAttachment.create(
                1L,
                "test1.jpg",
                "uuid_test1.jpg",
                "/uploads/comments/uuid_test1.jpg",
                "IMAGE",
                "image/jpeg",
                123L,
                1
        );
        ReflectionTestUtils.setField(attachment1, "id", 1L);
        ReflectionTestUtils.setField(attachment1, "createdAt", LocalDateTime.now());

        CommentAttachment attachment2 = CommentAttachment.create(
                1L,
                "test2.pdf",
                "uuid_test2.pdf",
                "/uploads/comments/uuid_test2.pdf",
                "FILE",
                "application/pdf",
                456L,
                2
        );
        ReflectionTestUtils.setField(attachment2, "id", 2L);
        ReflectionTestUtils.setField(attachment2, "createdAt", LocalDateTime.now());

        given(commentAttachmentRepository.findByCommentIdOrderByFileOrderAscIdAsc(1L))
                .willReturn(List.of(attachment1, attachment2));

        CommentAttachmentListResponse response = commentAttachmentService.getAttachments(1L);

        assertThat(response).isNotNull();
        assertThat(response.getAttachments()).hasSize(2);
        assertThat(response.getAttachments().get(0).getCommentId()).isEqualTo(1L);
        assertThat(response.getAttachments().get(0).getFileName()).isEqualTo("test1.jpg");
        assertThat(response.getAttachments().get(1).getFileName()).isEqualTo("test2.pdf");

        verify(commentRepository).findById(1L);
        verify(commentAttachmentRepository).findByCommentIdOrderByFileOrderAscIdAsc(1L);
    }

    @Test
    @DisplayName("댓글은 존재하지만 첨부가 없으면 빈 첨부 목록을 반환한다")
    void getAttachments_success_whenAttachmentEmpty() {
        // given
        Long commentId = 1L;
        given(commentRepository.findById(commentId)).willReturn(Optional.of(mock(Comment.class)));
        given(commentAttachmentRepository.findByCommentIdOrderByFileOrderAscIdAsc(commentId))
                .willReturn(List.of());

        // when
        CommentAttachmentListResponse response = commentAttachmentService.getAttachments(commentId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAttachments()).isEmpty();

        verify(commentRepository).findById(commentId);
        verify(commentAttachmentRepository).findByCommentIdOrderByFileOrderAscIdAsc(commentId);
    }

    @Test
    @DisplayName("존재하지 않는 댓글이면 첨부 목록 조회 시 예외 발생")
    void getAttachments_fail_whenCommentNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentAttachmentService.getAttachments(999L))
                // 현재 서비스는 존재하지 않는 댓글 조회 시 공통 예외 ApiException을 발생시킴
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("댓글을 찾을 수 없습니다.");

        verify(commentRepository).findById(999L);
        verify(commentAttachmentRepository, never()).findByCommentIdOrderByFileOrderAscIdAsc(999L);
    }

}
