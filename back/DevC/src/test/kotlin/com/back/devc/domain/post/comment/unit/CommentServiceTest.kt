package com.back.devc.domain.post.comment.unit

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentResponse
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService
import com.back.devc.domain.post.comment.dto.CommentCreateRequest
import com.back.devc.domain.post.comment.dto.CommentUpdateRequest
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.comment.service.CommentService
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.domain.post.post.service.PostService
import com.back.devc.global.exception.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
internal class CommentServiceTest {

    @Mock
    private lateinit var commentRepository: CommentRepository

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var commentAttachmentService: CommentAttachmentService

    @Mock
    private lateinit var postService: PostService

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var commentService: CommentService

    @Test
    @DisplayName("댓글 작성 성공 시 댓글을 저장하고 댓글 수 증가 및 댓글 생성 이벤트를 발행한다")
    fun createCommentSuccess() {
        val loginUserId = 2L
        val postId = 10L
        val requestDto = CommentCreateRequest("첫 댓글입니다.")
        val post = mock(Post::class.java)
        val member = mock(Member::class.java)

        `when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        `when`(post.title).thenReturn("테스트 게시글")
        `when`(memberRepository.findById(loginUserId)).thenReturn(Optional.of(member))
        `when`(member.userId).thenReturn(loginUserId)
        `when`(member.nickname).thenReturn("작성자B")
        `when`(commentAttachmentService.getAttachments(1L))
            .thenReturn(emptyAttachmentListResponse())
        `when`(commentRepository.save(anyNotNull()))
            .thenAnswer { invocation ->
                invocation.getArgument<Comment>(0).apply {
                    setCommentId(this, 1L)
                    setTimestamps(this)
                }
            }

        val response = commentService.createComment(postId, loginUserId, requestDto)

        assertThat(response).isNotNull()
        assertThat(response.commentId).isEqualTo(1L)
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.userId).isEqualTo(loginUserId)
        assertThat(response.parentCommentId).isNull()
        assertThat(response.content).isEqualTo("첫 댓글입니다.")
        verify(postService).increaseCommentCount(postId)

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        assertThat(eventCaptor.value.javaClass.simpleName).isEqualTo("CommentCreatedEvent")
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 댓글 생성 이벤트가 발행되지 않는다")
    fun createCommentFailWhenPostNotFoundDoesNotPublishEvent() {
        val loginUserId = 2L
        val postId = 999L
        val requestDto = CommentCreateRequest("존재하지 않는 게시글 댓글")

        `when`(postRepository.findById(postId)).thenReturn(Optional.empty())

        assertThrows<ApiException> {
            commentService.createComment(postId, loginUserId, requestDto)
        }
        verify(commentRepository, never()).save(anyNotNull())
        verify(postService, never()).increaseCommentCount(anyLong())
        verify(eventPublisher, never()).publishEvent(anyNotNull<Any>())
    }

    @Test
    @DisplayName("대댓글을 작성할 수 있다")
    fun createReplySuccess() {
        val loginUserId = 2L
        val parentCommentId = 100L
        val postId = 10L
        val requestDto = CommentCreateRequest("대댓글입니다.")
        val parentComment = createComment(
            id = parentCommentId,
            postId = postId,
            userId = 1L,
            parentCommentId = null,
            content = "부모 댓글",
        )
        val post = mock(Post::class.java)
        val member = mock(Member::class.java)

        `when`(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment))
        `when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        `when`(post.title).thenReturn("테스트 게시글")
        `when`(memberRepository.findById(loginUserId)).thenReturn(Optional.of(member))
        `when`(member.userId).thenReturn(loginUserId)
        `when`(member.nickname).thenReturn("작성자B")
        `when`(commentAttachmentService.getAttachments(200L))
            .thenReturn(emptyAttachmentListResponse())
        `when`(commentRepository.save(anyNotNull()))
            .thenAnswer { invocation ->
                invocation.getArgument<Comment>(0).apply {
                    setCommentId(this, 200L)
                    setTimestamps(this)
                }
            }

        val response = commentService.createReply(parentCommentId, loginUserId, requestDto)

        assertThat(response).isNotNull()
        assertThat(response.commentId).isEqualTo(200L)
        assertThat(response.parentCommentId).isEqualTo(parentCommentId)
        assertThat(response.content).isEqualTo("대댓글입니다.")
        verify(postService).increaseCommentCount(postId)

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        assertThat(eventCaptor.value.javaClass.simpleName).isEqualTo("ReplyCreatedEvent")
    }

    @Test
    @DisplayName("삭제된 댓글에는 답글을 작성할 수 없다")
    fun createReplyFailWhenParentDeleted() {
        val parentCommentId = 100L
        val deletedParentComment = createComment(
            id = parentCommentId,
            postId = 10L,
            userId = 1L,
            parentCommentId = null,
            content = "삭제 전 댓글",
        )
        deletedParentComment.softDelete()

        `when`(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(deletedParentComment))

        assertThrows<ApiException> {
            commentService.createReply(parentCommentId, 2L, CommentCreateRequest("대댓글"))
        }
        verify(eventPublisher, never()).publishEvent(anyNotNull<Any>())
        verify(commentRepository, never()).save(anyNotNull())
        verify(postService, never()).increaseCommentCount(anyLong())
    }

    @Test
    @DisplayName("댓글을 수정할 수 있다")
    fun updateCommentSuccess() {
        val commentId = 1L
        val loginUserId = 2L
        val postId = 10L
        val requestDto = CommentUpdateRequest("수정된 댓글")
        val comment = createComment(
            id = commentId,
            postId = postId,
            userId = loginUserId,
            parentCommentId = null,
            content = "기존 댓글",
        )
        val post = mock(Post::class.java)
        val member = mock(Member::class.java)

        `when`(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))
        `when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        `when`(post.title).thenReturn("테스트 게시글")
        `when`(memberRepository.findById(loginUserId)).thenReturn(Optional.of(member))
        `when`(member.nickname).thenReturn("작성자B")
        `when`(commentAttachmentService.getAttachments(commentId))
            .thenReturn(emptyAttachmentListResponse())

        val response = commentService.updateComment(commentId, loginUserId, requestDto)

        assertThat(comment.content).isEqualTo("수정된 댓글")
        assertThat(response.content).isEqualTo("수정된 댓글")
        assertThat(response.commentId).isEqualTo(commentId)
    }

    @Test
    @DisplayName("댓글을 삭제할 수 있다")
    fun deleteCommentSuccess() {
        val commentId = 1L
        val loginUserId = 2L
        val comment = createComment(
            id = commentId,
            postId = 10L,
            userId = loginUserId,
            parentCommentId = null,
            content = "삭제할 댓글",
        )

        `when`(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))

        val response = commentService.deleteComment(commentId, loginUserId)

        assertThat(comment.isDeleted).isTrue()
        assertThat(comment.content).isEqualTo("삭제된 댓글입니다.")
        assertThat(response.commentId).isEqualTo(commentId)
        assertThat(response.message).isEqualTo("댓글 삭제 성공")
        verify(postService).decreaseCommentCount(comment.postId!!)
    }

    @Test
    @DisplayName("게시글의 댓글 목록을 조회할 수 있다")
    fun getCommentsSuccess() {
        val postId = 10L
        val post = mock(Post::class.java)
        val parent = createComment(
            id = 1L,
            postId = postId,
            userId = 1L,
            parentCommentId = null,
            content = "부모 댓글",
        )
        val parentWriter = mock(Member::class.java)

        `when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        `when`(post.title).thenReturn("테스트 게시글")
        `when`(memberRepository.findById(1L)).thenReturn(Optional.of(parentWriter))
        `when`(parentWriter.nickname).thenReturn("작성자A")
        `when`(
            commentRepository.findByPostIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(
                eq(postId),
                anyNotNull(),
            ),
        ).thenReturn(PageImpl(listOf(parent), PageRequest.of(0, 20), 1))
        `when`(commentAttachmentService.getAttachments(1L))
            .thenReturn(emptyAttachmentListResponse())

        val response = commentService.getComments(postId, 0, 20)

        assertThat(response).isNotNull()
        assertThat(response.comments).hasSize(1)
        assertThat(response.page).isEqualTo(0)
        assertThat(response.size).isEqualTo(20)
        assertThat(response.totalElements).isEqualTo(1)
        assertThat(response.totalPages).isEqualTo(1)
        assertThat(response.hasNext).isFalse()
        assertThat(response.comments[0].commentId).isEqualTo(1L)
        assertThat(response.comments[0].replies).isEmpty()
    }

    private fun createComment(
        id: Long,
        postId: Long,
        userId: Long,
        parentCommentId: Long?,
        content: String,
    ): Comment {
        return Comment(postId, userId, parentCommentId, content).apply {
            setCommentId(this, id)
            setTimestamps(this)
        }
    }

    private fun emptyAttachmentListResponse(): CommentAttachmentListResponse {
        return CommentAttachmentListResponse(emptyList())
    }

    private fun setCommentId(
        comment: Comment,
        id: Long,
    ) {
        ReflectionTestUtils.setField(comment, "id", id)
    }

    private fun setTimestamps(comment: Comment) {
        val now = LocalDateTime.now()
        ReflectionTestUtils.setField(comment, "createdAt", now)
        ReflectionTestUtils.setField(comment, "updatedAt", now)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNotNull(): T {
        any<T>()
        return null as T
    }
}