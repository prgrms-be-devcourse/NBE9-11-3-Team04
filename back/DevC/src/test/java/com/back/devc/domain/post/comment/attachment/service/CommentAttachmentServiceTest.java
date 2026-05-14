package com.back.devc.domain.post.comment.attachment.service;

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse;
import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment;
import com.back.devc.domain.post.comment.attachment.repository.CommentAttachmentRepository;
import com.back.devc.domain.post.comment.entity.Comment;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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

        given(commentAttachmentRepository.findByCommentIdOrderByFileOrderAscIdAsc(1L))
                .willReturn(List.of(attachment1, attachment2));

        CommentAttachmentListResponse response = commentAttachmentService.getAttachments(1L);

        assertThat(response).isNotNull();
        assertThat(response.attachments()).hasSize(2);
        assertThat(response.attachments().get(0).commentId()).isEqualTo(1L);
        assertThat(response.attachments().get(0).fileName()).isEqualTo("test1.jpg");
        assertThat(response.attachments().get(1).fileName()).isEqualTo("test2.pdf");

        verify(commentRepository).findById(1L);
        verify(commentAttachmentRepository).findByCommentIdOrderByFileOrderAscIdAsc(1L);
    }

    @Test
    @DisplayName("존재하지 않는 댓글이면 첨부 목록 조회 시 예외 발생")
    void getAttachments_fail_whenCommentNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentAttachmentService.getAttachments(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("댓글을 찾을 수 없습니다.");
    }

}
