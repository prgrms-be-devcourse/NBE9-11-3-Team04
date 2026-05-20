package com.back.devc.domain.interaction.notification.unit

import com.back.devc.domain.interaction.notification.entity.Notification
import com.back.devc.domain.interaction.notification.repository.NotificationRepository
import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

@ExtendWith(MockitoExtension::class)
internal class NotificationServiceTest {

    @Mock
    private lateinit var notificationRepository: NotificationRepository

    @Mock
    private lateinit var commentRepository: CommentRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var postRepository: PostRepository

    @InjectMocks
    private lateinit var notificationService: NotificationService

    @Test
    @DisplayName("내 알림 목록을 조회할 수 있다")
    fun getMyNotificationsSuccess() {
        val loginUserId = 1L
        val actorUserId = 2L
        val notification = mockNotification(
            id = 10L,
            userId = loginUserId,
            actorUserId = actorUserId,
            postId = 100L,
            commentId = 200L,
            type = "COMMENT",
            message = "댓글 알림 메시지",
            isRead = false,
        )
        val actor = Mockito.mock(Member::class.java)

        Mockito.`when`(actor.userId).thenReturn(actorUserId)
        Mockito.`when`(actor.nickname).thenReturn("작성자B")
        Mockito.`when`(
            notificationRepository.findAvailableByUserIdOrderByCreatedAtDesc(
                ArgumentMatchers.eq(loginUserId),
                anyNotNull(),
            ),
        ).thenReturn(PageImpl(listOf(notification), PageRequest.of(0, 20), 1))
        Mockito.`when`(memberRepository.findAllById(listOf(actorUserId)))
            .thenReturn(listOf(actor))

        val response = notificationService.getMyNotifications(loginUserId, 0, 20, "all")

        Assertions.assertNotNull(response)
        org.assertj.core.api.Assertions.assertThat(response.notifications).hasSize(1)
        org.assertj.core.api.Assertions.assertThat(response.page).isEqualTo(0)
        org.assertj.core.api.Assertions.assertThat(response.size).isEqualTo(20)
        org.assertj.core.api.Assertions.assertThat(response.totalElements).isEqualTo(1)
        org.assertj.core.api.Assertions.assertThat(response.totalPages).isEqualTo(1)
        org.assertj.core.api.Assertions.assertThat(response.hasNext).isFalse()
        Mockito.verify(notificationRepository).findAvailableByUserIdOrderByCreatedAtDesc(
            ArgumentMatchers.eq(loginUserId),
            anyNotNull(),
        )
        Mockito.verify(memberRepository).findAllById(listOf(actorUserId))
    }

    @Test
    @DisplayName("본인 알림은 읽음 처리할 수 있다")
    fun readNotificationSuccess() {
        val notificationId = 1L
        val loginUserId = 10L
        val actorUserId = 20L
        val read = AtomicBoolean(false)
        val notification = mockNotification(
            id = notificationId,
            userId = loginUserId,
            actorUserId = actorUserId,
            postId = 100L,
            commentId = 200L,
            type = "COMMENT",
            message = "댓글 알림 메시지",
            isRead = false,
        )
        val actor = Mockito.mock(Member::class.java)
        val post = Mockito.mock(Post::class.java)

        Mockito.`when`(notification.isRead).thenAnswer { read.get() }
        Mockito.doAnswer {
            read.set(true)
            null
        }.`when`(notification).markAsRead()
        Mockito.`when`(actor.nickname).thenReturn("작성자B")
        Mockito.`when`(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification))
        Mockito.`when`(postRepository.findById(100L))
            .thenReturn(Optional.of(post))
        Mockito.`when`(post.isDeleted).thenReturn(false)
        Mockito.`when`(memberRepository.findById(actorUserId))
            .thenReturn(Optional.of(actor))

        val response = notificationService.readNotification(notificationId, loginUserId)

        Assertions.assertNotNull(response)
        org.assertj.core.api.Assertions.assertThat(read.get()).isTrue()
        Mockito.verify(notification).markAsRead()
    }

    @Test
    @DisplayName("다른 사람 알림은 읽음 처리할 수 없다")
    fun readNotificationFailWhenOtherUsersNotification() {
        val notificationId = 1L
        val loginUserId = 10L
        val notification = Mockito.mock(Notification::class.java)

        Mockito.`when`(notification.userId).thenReturn(999L)
        Mockito.`when`(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification))

        assertThrows<ApiException> {
            notificationService.readNotification(notificationId, loginUserId)
        }
    }

    @Test
    @DisplayName("다른 사용자가 내 게시글에 댓글을 작성하면 댓글 알림이 생성된다")
    fun createCommentNotificationSuccess() {
        val postId = 100L
        val actorUserId = 2L
        val commentId = 300L
        val postOwnerId = 1L
        val post = Mockito.mock(Post::class.java)
        val owner = Mockito.mock(Member::class.java)
        val actor = Mockito.mock(Member::class.java)
        val captor = ArgumentCaptor.forClass(Notification::class.java)

        Mockito.`when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        Mockito.`when`(post.member).thenReturn(owner)
        Mockito.`when`(owner.userId).thenReturn(postOwnerId)
        Mockito.`when`(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor))
        Mockito.`when`(actor.nickname).thenReturn("작성자B")

        notificationService.createCommentNotification(postId, actorUserId, commentId)

        Mockito.verify(notificationRepository).save(captor.capture())
        val saved = captor.value

        org.assertj.core.api.Assertions.assertThat(saved.userId).isEqualTo(postOwnerId)
        org.assertj.core.api.Assertions.assertThat(saved.actorUserId).isEqualTo(actorUserId)
        org.assertj.core.api.Assertions.assertThat(saved.postId).isEqualTo(postId)
        org.assertj.core.api.Assertions.assertThat(saved.commentId).isEqualTo(commentId)
        org.assertj.core.api.Assertions.assertThat(saved.type).isEqualTo("COMMENT")
        org.assertj.core.api.Assertions.assertThat(saved.message).contains("게시글에 댓글을 남겼습니다")
    }

    @Test
    @DisplayName("내가 내 게시글에 댓글을 작성하면 댓글 알림은 생성되지 않는다")
    fun createCommentNotificationNoNotificationWhenSelfComment() {
        val postId = 100L
        val actorUserId = 1L
        val commentId = 300L
        val post = Mockito.mock(Post::class.java)
        val owner = Mockito.mock(Member::class.java)

        Mockito.`when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        Mockito.`when`(post.member).thenReturn(owner)
        Mockito.`when`(owner.userId).thenReturn(actorUserId)

        notificationService.createCommentNotification(postId, actorUserId, commentId)

        Mockito.verify(notificationRepository, Mockito.never()).save(anyNotNull())
    }

    @Test
    @DisplayName("다른 사용자가 내 댓글에 답글을 작성하면 답글 알림이 생성된다")
    fun createReplyNotificationSuccess() {
        val parentCommentId = 10L
        val actorUserId = 2L
        val replyCommentId = 20L
        val receiverUserId = 1L
        val parentComment = Mockito.mock(Comment::class.java)
        val actor = Mockito.mock(Member::class.java)
        val captor = ArgumentCaptor.forClass(Notification::class.java)

        Mockito.`when`(commentRepository.findById(parentCommentId))
            .thenReturn(Optional.of(parentComment))
        Mockito.`when`(parentComment.isDeleted).thenReturn(false)
        Mockito.`when`(parentComment.getUserId()).thenReturn(receiverUserId)
        Mockito.`when`(parentComment.postId).thenReturn(100L)
        Mockito.`when`(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor))
        Mockito.`when`(actor.nickname).thenReturn("작성자B")

        notificationService.createReplyNotification(parentCommentId, actorUserId, replyCommentId)

        Mockito.verify(notificationRepository).save(captor.capture())
        val saved = captor.value

        org.assertj.core.api.Assertions.assertThat(saved.userId).isEqualTo(receiverUserId)
        org.assertj.core.api.Assertions.assertThat(saved.actorUserId).isEqualTo(actorUserId)
        org.assertj.core.api.Assertions.assertThat(saved.postId).isEqualTo(100L)
        org.assertj.core.api.Assertions.assertThat(saved.commentId).isEqualTo(replyCommentId)
        org.assertj.core.api.Assertions.assertThat(saved.type).isEqualTo("REPLY")
        org.assertj.core.api.Assertions.assertThat(saved.message).contains("답글을 남겼습니다")
    }

    @Test
    @DisplayName("내 댓글에 내가 답글을 작성하면 답글 알림은 생성되지 않는다")
    fun createReplyNotificationNoNotificationWhenSelfReply() {
        val parentCommentId = 10L
        val actorUserId = 1L
        val replyCommentId = 20L
        val parentComment = Mockito.mock(Comment::class.java)

        Mockito.`when`(commentRepository.findById(parentCommentId))
            .thenReturn(Optional.of(parentComment))
        Mockito.`when`(parentComment.isDeleted).thenReturn(false)
        Mockito.`when`(parentComment.getUserId()).thenReturn(actorUserId)

        notificationService.createReplyNotification(parentCommentId, actorUserId, replyCommentId)

        Mockito.verify(notificationRepository, Mockito.never()).save(anyNotNull())
        Mockito.verify(memberRepository, Mockito.never()).findById(actorUserId)
    }

    @Test
    @DisplayName("삭제된 부모 댓글에는 답글 알림이 생성되지 않는다")
    fun createReplyNotificationNoNotificationWhenParentCommentDeleted() {
        val parentCommentId = 10L
        val parentComment = Mockito.mock(Comment::class.java)

        Mockito.`when`(commentRepository.findById(parentCommentId))
            .thenReturn(Optional.of(parentComment))
        Mockito.`when`(parentComment.isDeleted).thenReturn(true)

        notificationService.createReplyNotification(parentCommentId, 2L, 20L)

        Mockito.verify(notificationRepository, Mockito.never()).save(anyNotNull())
    }

    @Test
    @DisplayName("다른 사용자가 내 게시글에 좋아요를 누르면 좋아요 알림이 생성된다")
    fun createPostLikeNotificationSuccess() {
        val postId = 100L
        val actorUserId = 2L
        val postOwnerId = 1L
        val post = Mockito.mock(Post::class.java)
        val owner = Mockito.mock(Member::class.java)
        val actor = Mockito.mock(Member::class.java)
        val captor = ArgumentCaptor.forClass(Notification::class.java)

        Mockito.`when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        Mockito.`when`(post.member).thenReturn(owner)
        Mockito.`when`(owner.userId).thenReturn(postOwnerId)
        Mockito.`when`(
            notificationRepository.existsByUserIdAndActorUserIdAndPostIdAndType(
                postOwnerId,
                actorUserId,
                postId,
                "LIKE",
            ),
        ).thenReturn(false)
        Mockito.`when`(memberRepository.findById(actorUserId)).thenReturn(Optional.of(actor))
        Mockito.`when`(actor.nickname).thenReturn("작성자B")

        notificationService.createPostLikeNotification(postId, actorUserId)

        Mockito.verify(notificationRepository).save(captor.capture())
        val saved = captor.value

        org.assertj.core.api.Assertions.assertThat(saved.userId).isEqualTo(postOwnerId)
        org.assertj.core.api.Assertions.assertThat(saved.actorUserId).isEqualTo(actorUserId)
        org.assertj.core.api.Assertions.assertThat(saved.postId).isEqualTo(postId)
        org.assertj.core.api.Assertions.assertThat(saved.type).isEqualTo("LIKE")
        org.assertj.core.api.Assertions.assertThat(saved.message).contains("좋아합니다")
    }

    @Test
    @DisplayName("내가 내 게시글에 좋아요를 누르면 좋아요 알림은 생성되지 않는다")
    fun createPostLikeNotificationNoNotificationWhenSelfLike() {
        val postId = 100L
        val actorUserId = 1L
        val post = Mockito.mock(Post::class.java)
        val owner = Mockito.mock(Member::class.java)

        Mockito.`when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        Mockito.`when`(post.member).thenReturn(owner)
        Mockito.`when`(owner.userId).thenReturn(actorUserId)

        notificationService.createPostLikeNotification(postId, actorUserId)

        Mockito.verify(notificationRepository, Mockito.never()).save(anyNotNull())
    }

    @Test
    @DisplayName("같은 사용자가 같은 게시글에 다시 좋아요를 눌러도 중복 알림은 생성되지 않는다")
    fun createPostLikeNotificationNoDuplicateNotification() {
        val postId = 100L
        val actorUserId = 2L
        val postOwnerId = 1L
        val post = Mockito.mock(Post::class.java)
        val owner = Mockito.mock(Member::class.java)

        Mockito.`when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        Mockito.`when`(post.member).thenReturn(owner)
        Mockito.`when`(owner.userId).thenReturn(postOwnerId)
        Mockito.`when`(
            notificationRepository.existsByUserIdAndActorUserIdAndPostIdAndType(
                postOwnerId,
                actorUserId,
                postId,
                "LIKE",
            ),
        ).thenReturn(true)

        notificationService.createPostLikeNotification(postId, actorUserId)

        Mockito.verify(notificationRepository, Mockito.never()).save(anyNotNull())
    }

    @Test
    @DisplayName("관리자 처리 후 게시글 신고 알림이 생성된다")
    fun createPostReportNotificationSuccess() {
        val postId = 100L
        val adminUserId = 99L
        val postOwnerId = 1L
        val post = Mockito.mock(Post::class.java)
        val owner = Mockito.mock(Member::class.java)
        val captor = ArgumentCaptor.forClass(Notification::class.java)

        Mockito.`when`(postRepository.findById(postId)).thenReturn(Optional.of(post))
        Mockito.`when`(post.member).thenReturn(owner)
        Mockito.`when`(owner.userId).thenReturn(postOwnerId)

        notificationService.createPostReportNotification(postId, adminUserId)

        Mockito.verify(notificationRepository).save(captor.capture())
        val saved = captor.value

        org.assertj.core.api.Assertions.assertThat(saved.userId).isEqualTo(postOwnerId)
        org.assertj.core.api.Assertions.assertThat(saved.actorUserId).isEqualTo(adminUserId)
        org.assertj.core.api.Assertions.assertThat(saved.postId).isEqualTo(postId)
        org.assertj.core.api.Assertions.assertThat(saved.type).isEqualTo("REPORT")
    }

    @Test
    @DisplayName("관리자 처리 후 댓글 신고 알림이 생성된다")
    fun createCommentReportNotificationSuccess() {
        val commentId = 200L
        val adminUserId = 99L
        val commentOwnerId = 1L
        val comment = Mockito.mock(Comment::class.java)
        val captor = ArgumentCaptor.forClass(Notification::class.java)

        Mockito.`when`(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))
        Mockito.`when`(comment.getUserId()).thenReturn(commentOwnerId)

        notificationService.createCommentReportNotification(commentId, adminUserId)

        Mockito.verify(notificationRepository).save(captor.capture())
        val saved = captor.value

        org.assertj.core.api.Assertions.assertThat(saved.userId).isEqualTo(commentOwnerId)
        org.assertj.core.api.Assertions.assertThat(saved.actorUserId).isEqualTo(adminUserId)
        org.assertj.core.api.Assertions.assertThat(saved.commentId).isEqualTo(commentId)
        org.assertj.core.api.Assertions.assertThat(saved.type).isEqualTo("REPORT")
    }

    private fun mockNotification(
        id: Long,
        userId: Long,
        actorUserId: Long,
        postId: Long,
        commentId: Long,
        type: String,
        message: String,
        isRead: Boolean,
    ): Notification {
        val notification = Mockito.mock(Notification::class.java)
        val createdAt = LocalDateTime.now()

        Mockito.`when`(notification.id).thenReturn(id)
        Mockito.`when`(notification.userId).thenReturn(userId)
        Mockito.`when`(notification.actorUserId).thenReturn(actorUserId)
        Mockito.`when`(notification.postId).thenReturn(postId)
        Mockito.`when`(notification.commentId).thenReturn(commentId)
        Mockito.`when`(notification.type).thenReturn(type)
        Mockito.`when`(notification.message).thenReturn(message)
        Mockito.`when`(notification.isRead).thenReturn(isRead)
        Mockito.`when`(notification.createdAt).thenReturn(createdAt)

        return notification
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNotNull(): T {
        ArgumentMatchers.any<T>()
        return null as T
    }
}