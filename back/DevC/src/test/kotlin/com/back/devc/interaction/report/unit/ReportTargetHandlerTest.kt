package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.util.ReportTargetHandler
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.member.service.MemberSanctionService
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.interaction.report.toOptional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ReportTargetHandler")
internal class ReportTargetHandlerTest {
    private val postRepository = mock<PostRepository>()
    private val commentRepository = mock<CommentRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val notificationService = mock<NotificationService>()
    private val memberSanctionService = mock<MemberSanctionService>()

    private val handler = ReportTargetHandler(
        postRepository = postRepository,
        commentRepository = commentRepository,
        memberRepository = memberRepository,
        notificationService = notificationService,
        memberSanctionService = memberSanctionService
    )

    private lateinit var admin: Member

    @BeforeEach
    fun setUp() {
        admin = mock()
        whenever(admin.userId).thenReturn(ADMIN_ID)
    }

    @Nested
    @DisplayName("handleApproved")
    inner class HandleApproved {

        @Test
        @DisplayName("deletes post and sends notification without sanction")
        fun handleApproved_post_noSanction() {
            val post = mock<Post>()
            whenever(post.isDeleted).thenReturn(false)
            whenever(postRepository.findById(POST_ID)).thenReturn(post.toOptional())

            handler.handleApproved(TargetType.POST, POST_ID, admin, null, null)

            verify(notificationService).createPostReportNotification(POST_ID, ADMIN_ID)
            verify(postRepository).findById(POST_ID)
            verify(post).delete()
            verifyNoInteractions(memberSanctionService)
        }

        @Test
        @DisplayName("soft deletes comment and applies warning sanction")
        fun handleApproved_comment_withSanction() {
            val comment = mock<Comment>()
            val writer = mock<Member>()
            whenever(comment.isDeleted).thenReturn(false)
            whenever(comment.getUserId()).thenReturn(WRITER_ID)
            whenever(commentRepository.findById(COMMENT_ID)).thenReturn(comment.toOptional())
            whenever(memberRepository.findById(WRITER_ID)).thenReturn(writer.toOptional())

            handler.handleApproved(TargetType.COMMENT, COMMENT_ID, admin, SanctionType.WARNED, 0)

            verify(notificationService).createCommentReportNotification(COMMENT_ID, ADMIN_ID)
            verify(comment).softDelete()
            verify(memberSanctionService).apply(writer, MemberStatus.WARNED, 0)
        }

        @Test
        @DisplayName("does not delete an already deleted post")
        fun deleteTarget_post_alreadyDeleted() {
            val post = mock<Post>()
            whenever(post.isDeleted).thenReturn(true)
            whenever(postRepository.findById(POST_ID)).thenReturn(post.toOptional())

            handler.handleApproved(TargetType.POST, POST_ID, admin, null, null)

            verify(post, never()).delete()
        }

        @Test
        @DisplayName("soft deletes comment target")
        fun deleteTarget_comment_success() {
            val comment = mock<Comment>()
            whenever(comment.isDeleted).thenReturn(false)
            whenever(commentRepository.findById(COMMENT_ID)).thenReturn(comment.toOptional())

            handler.handleApproved(TargetType.COMMENT, COMMENT_ID, admin, null, null)

            verify(comment).softDelete()
        }

        @Test
        @DisplayName("applies suspended sanction to post writer")
        fun applySanction_suspended() {
            val writer = mock<Member>()
            val post = mock<Post>()
            whenever(post.member).thenReturn(writer)
            whenever(postRepository.findById(POST_ID)).thenReturn(post.toOptional())

            handler.handleApproved(TargetType.POST, POST_ID, admin, SanctionType.SUSPENDED, 7)

            verify(memberSanctionService).apply(writer, MemberStatus.SUSPENDED, 7)
        }

        @Test
        @DisplayName("throws when post target member cannot be resolved")
        fun findTargetMember_post_notFound() {
            whenever(postRepository.findById(POST_ID)).thenReturn(null.toOptional())

            assertThatThrownBy {
                handler.handleApproved(TargetType.POST, POST_ID, admin, SanctionType.WARNED, null)
            }.isInstanceOf(ApiException::class.java)
        }

        @Test
        @DisplayName("throws when comment writer cannot be resolved")
        fun findTargetMember_comment_memberNotFound() {
            val comment = mock<Comment>()
            whenever(comment.getUserId()).thenReturn(WRITER_ID)
            whenever(commentRepository.findById(COMMENT_ID)).thenReturn(comment.toOptional())
            whenever(memberRepository.findById(WRITER_ID)).thenReturn(null.toOptional())

            assertThatThrownBy {
                handler.handleApproved(TargetType.COMMENT, COMMENT_ID, admin, SanctionType.WARNED, null)
            }.isInstanceOf(ApiException::class.java)
        }
    }

    @Test
    @DisplayName("handleRejected sends post report notification")
    fun handleRejected_success() {
        handler.handleRejected(TargetType.POST, POST_ID, admin)

        verify(notificationService).createPostReportNotification(POST_ID, ADMIN_ID)
    }

    @Nested
    @DisplayName("exists")
    inner class Exists {

        @Test
        @DisplayName("returns true when post exists")
        fun exists_post_true() {
            whenever(postRepository.existsById(POST_ID)).thenReturn(true)

            val result = handler.exists(TargetType.POST, POST_ID)

            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("returns false when comment does not exist")
        fun exists_comment_false() {
            whenever(commentRepository.existsById(COMMENT_ID)).thenReturn(false)

            val result = handler.exists(TargetType.COMMENT, COMMENT_ID)

            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("getTargetInfo")
    inner class GetTargetInfo {

        @Test
        @DisplayName("returns post writer and content summary")
        fun getTargetInfo_post_success() {
            val writer = mock<Member>()
            val post = mock<Post>()
            whenever(writer.nickname).thenReturn("post-writer")
            whenever(post.member).thenReturn(writer)
            whenever(post.title).thenReturn("post title")
            whenever(post.content).thenReturn("post content")
            whenever(postRepository.findById(POST_ID)).thenReturn(post.toOptional())

            val result = handler.getTargetInfo(TargetType.POST, POST_ID)

            assertThat(result.nickname).isEqualTo("post-writer")
            assertThat(result.title).isEqualTo("post title")
            assertThat(result.content).isEqualTo("post content")
        }

        @Test
        @DisplayName("returns comment writer and content summary")
        fun getTargetInfo_comment_success() {
            val comment = mock<Comment>()
            val writer = mock<Member>()
            whenever(comment.getUserId()).thenReturn(WRITER_ID)
            whenever(comment.content).thenReturn("comment content")
            whenever(commentRepository.findById(COMMENT_ID)).thenReturn(comment.toOptional())
            whenever(writer.nickname).thenReturn("comment-writer")
            whenever(memberRepository.findById(WRITER_ID)).thenReturn(writer.toOptional())

            val result = handler.getTargetInfo(TargetType.COMMENT, COMMENT_ID)

            assertThat(result.nickname).isEqualTo("comment-writer")
            assertThat(result.title).isNull()
            assertThat(result.content).isEqualTo("comment content")
        }

        @Test
        @DisplayName("returns null fields when target is missing")
        fun getTargetInfo_missingTarget_returnsNullFields() {
            whenever(postRepository.findById(POST_ID)).thenReturn(null.toOptional())

            val result = handler.getTargetInfo(TargetType.POST, POST_ID)

            assertThat(result.nickname).isNull()
            assertThat(result.title).isNull()
            assertThat(result.content).isNull()
        }
    }

    private companion object {
        const val ADMIN_ID = 1L
        const val POST_ID = 10L
        const val COMMENT_ID = 20L
        const val WRITER_ID = 99L
    }
}
