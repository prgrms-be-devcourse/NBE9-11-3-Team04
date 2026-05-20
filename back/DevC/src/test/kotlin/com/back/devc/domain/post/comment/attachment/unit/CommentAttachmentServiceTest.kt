package com.back.devc.domain.post.comment.attachment.unit

import com.back.devc.toRepositoryResult

import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment
import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment.Companion.create
import com.back.devc.domain.post.comment.attachment.repository.CommentAttachmentRepository
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.global.exception.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
internal class CommentAttachmentServiceTest {

    @Mock
    private lateinit var commentAttachmentRepository: CommentAttachmentRepository

    @Mock
    private lateinit var commentRepository: CommentRepository

    @InjectMocks
    private lateinit var commentAttachmentService: CommentAttachmentService

    @Test
    @DisplayName("댓글 첨부 목록 조회 성공")
    fun getAttachmentsSuccess() {
        val comment = mock(Comment::class.java)
        val attachment1 = createCommentAttachment(
            id = 1L,
            commentId = 1L,
            fileName = "test1.jpg",
            storedFileName = "uuid_test1.jpg",
            filePath = "/uploads/comments/uuid_test1.jpg",
            fileType = "IMAGE",
            contentType = "image/jpeg",
            fileSize = 123L,
            fileOrder = 1,
        )
        val attachment2 = createCommentAttachment(
            id = 2L,
            commentId = 1L,
            fileName = "test2.pdf",
            storedFileName = "uuid_test2.pdf",
            filePath = "/uploads/comments/uuid_test2.pdf",
            fileType = "FILE",
            contentType = "application/pdf",
            fileSize = 456L,
            fileOrder = 2,
        )

        given(commentRepository.findById(1L))
            .willReturn(comment.toRepositoryResult())
        given(commentAttachmentRepository.findByCommentIdOrderByFileOrderAscIdAsc(1L))
            .willReturn(listOf(attachment1, attachment2))

        val response = commentAttachmentService.getAttachments(1L)

        assertThat(response).isNotNull()
        assertThat(response.attachments).hasSize(2)
        assertThat(response.attachments[0].commentId).isEqualTo(1L)
        assertThat(response.attachments[0].fileName).isEqualTo("test1.jpg")
        assertThat(response.attachments[1].fileName).isEqualTo("test2.pdf")

        verify(commentRepository).findById(1L)
        verify(commentAttachmentRepository).findByCommentIdOrderByFileOrderAscIdAsc(1L)
    }

    @Test
    @DisplayName("댓글은 존재하지만 첨부가 없으면 빈 첨부 목록을 반환한다")
    fun getAttachmentsSuccessWhenAttachmentEmpty() {
        val commentId = 1L
        val comment = mock(Comment::class.java)

        given(commentRepository.findById(commentId))
            .willReturn(comment.toRepositoryResult())
        given(commentAttachmentRepository.findByCommentIdOrderByFileOrderAscIdAsc(commentId))
            .willReturn(emptyList())

        val response = commentAttachmentService.getAttachments(commentId)

        assertThat(response).isNotNull()
        assertThat(response.attachments).isEmpty()

        verify(commentRepository).findById(commentId)
        verify(commentAttachmentRepository).findByCommentIdOrderByFileOrderAscIdAsc(commentId)
    }

    @Test
    @DisplayName("존재하지 않는 댓글이면 첨부 목록 조회 시 예외 발생")
    fun getAttachmentsFailWhenCommentNotFound() {
        given(commentRepository.findById(999L))
            .willReturn(null.toRepositoryResult())

        assertThatThrownBy { commentAttachmentService.getAttachments(999L) }
            .isInstanceOf(ApiException::class.java)
            .hasMessageContaining("댓글을 찾을 수 없습니다.")

        verify(commentRepository).findById(999L)
        verify(commentAttachmentRepository, never()).findByCommentIdOrderByFileOrderAscIdAsc(999L)
    }

    private fun createCommentAttachment(
        id: Long,
        commentId: Long,
        fileName: String,
        storedFileName: String,
        filePath: String,
        fileType: String,
        contentType: String,
        fileSize: Long,
        fileOrder: Int,
    ): CommentAttachment {
        val attachment = create(
            commentId = commentId,
            fileName = fileName,
            storedName = storedFileName,
            fileUrl = filePath,
            fileType = fileType,
            mimeType = contentType,
            fileSize = fileSize,
            fileOrder = fileOrder,
        )
        ReflectionTestUtils.setField(attachment, "id", id)
        ReflectionTestUtils.setField(attachment, "createdAt", LocalDateTime.now())

        return attachment
    }
}
