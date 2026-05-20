package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.interaction.report.service.ReportGroupCreationService
import com.back.devc.domain.interaction.report.service.UserReportService
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.exception.errorCode.ReportErrorCode
import com.back.devc.interaction.report.toOptional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserReportService")
internal class UserReportServiceTest {
    private val reportRepository = mock<ReportRepository>()
    private val reportGroupRepository = mock<ReportGroupRepository>()
    private val reportGroupCreationService = mock<ReportGroupCreationService>()
    private val memberRepository = mock<MemberRepository>()
    private val postRepository = mock<PostRepository>()
    private val commentRepository = mock<CommentRepository>()

    private val userReportService = UserReportService(
        reportRepository = reportRepository,
        reportGroupRepository = reportGroupRepository,
        reportGroupCreationService = reportGroupCreationService,
        memberRepository = memberRepository,
        postRepository = postRepository,
        commentRepository = commentRepository
    )

    private lateinit var reporter: Member
    private lateinit var author: Member
    private lateinit var post: Post
    private lateinit var comment: Comment

    @BeforeEach
    fun setUp() {
        reporter = mock()
        author = mock()
        post = mock()
        comment = mock()

        whenever(reporter.userId).thenReturn(REPORTER_ID)
        whenever(author.userId).thenReturn(AUTHOR_ID)
    }

    @Nested
    @DisplayName("reportPost")
    inner class ReportPost {

        @Test
        @DisplayName("saves a pending post report when the target is valid")
        fun reportPost_savesReport() {
            val request = ReportRequestDTO(POST_ID, "SPAM", "Repeated promotion")
            givenExistingReporter()
            givenExistingPostByAnotherMember(deleted = false)
            givenDuplicateReportExists(TargetType.POST, POST_ID, exists = false)
            val reportGroup = givenNewReportGroup(TargetType.POST, POST_ID)

            userReportService.reportPost(REPORTER_ID, request)

            val savedReport = captureSavedReport()
            assertThat(savedReport.reporter).isSameAs(reporter)
            assertThat(savedReport.targetType).isEqualTo(TargetType.POST)
            assertThat(savedReport.targetId).isEqualTo(POST_ID)
            assertThat(savedReport.reasonType).isEqualTo("SPAM")
            assertThat(savedReport.reasonDetail).isEqualTo("Repeated promotion")
            assertThat(savedReport.reportGroup).isSameAs(reportGroup)
            assertThat(reportGroup.reportCount).isEqualTo(1L)
        }

        @Test
        @DisplayName("throws when reporter does not exist")
        fun reportPost_throwsWhenReporterMissing() {
            whenever(memberRepository.findById(REPORTER_ID)).thenReturn(null.toOptional())

            assertReportError(
                action = { userReportService.reportPost(REPORTER_ID, ReportRequestDTO(POST_ID, "SPAM", null)) },
                expectedErrorCode = MemberErrorCode.MEMBER_NOT_FOUND
            )
            verifyNoInteractions(postRepository, commentRepository, reportRepository, reportGroupRepository)
        }

        @Test
        @DisplayName("throws when post does not exist")
        fun reportPost_throwsWhenPostMissing() {
            givenExistingReporter()
            whenever(postRepository.findById(POST_ID)).thenReturn(null.toOptional())

            assertReportError(
                action = { userReportService.reportPost(REPORTER_ID, ReportRequestDTO(POST_ID, "SPAM", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_404_TARGET
            )
            verify(reportRepository, never()).save(any())
        }

        @Test
        @DisplayName("throws when reporter owns the post")
        fun reportPost_throwsWhenReportingOwnPost() {
            givenExistingReporter()
            whenever(post.member).thenReturn(reporter)
            whenever(postRepository.findById(POST_ID)).thenReturn(post.toOptional())

            assertReportError(
                action = { userReportService.reportPost(REPORTER_ID, ReportRequestDTO(POST_ID, "SPAM", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_400_REPORT_SELF
            )
            verify(reportRepository, never()).save(any())
        }

        @Test
        @DisplayName("throws when post is already deleted")
        fun reportPost_throwsWhenPostDeleted() {
            givenExistingReporter()
            givenExistingPostByAnotherMember(deleted = true)

            assertReportError(
                action = { userReportService.reportPost(REPORTER_ID, ReportRequestDTO(POST_ID, "SPAM", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_410_ALREADY_DELETED
            )
            verify(reportRepository, never()).save(any())
        }

        @Test
        @DisplayName("throws when reporter already reported the same post")
        fun reportPost_throwsWhenDuplicate() {
            givenExistingReporter()
            givenExistingPostByAnotherMember(deleted = false)
            givenDuplicateReportExists(TargetType.POST, POST_ID, exists = true)

            assertReportError(
                action = { userReportService.reportPost(REPORTER_ID, ReportRequestDTO(POST_ID, "SPAM", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_409_ALREADY_REPORT_USER
            )
            verify(reportRepository, never()).save(any())
        }

        @Test
        @DisplayName("recovers with existing report group when concurrent creation loses unique race")
        fun reportPost_usesExistingGroupAfterConcurrentCreateConflict() {
            val request = ReportRequestDTO(POST_ID, "SPAM", "Repeated promotion")
            givenExistingReporter()
            givenExistingPostByAnotherMember(deleted = false)
            givenDuplicateReportExists(TargetType.POST, POST_ID, exists = false)

            val existingReportGroup = ReportGroup(TargetType.POST, POST_ID, LocalDateTime.now())
            whenever(reportGroupRepository.findByTargetTypeAndTargetId(TargetType.POST, POST_ID))
                .thenReturn(null, existingReportGroup)
            whenever(reportGroupCreationService.createOpenReportGroup(any(), any(), any()))
                .thenThrow(DataIntegrityViolationException("duplicate report group"))

            userReportService.reportPost(REPORTER_ID, request)

            val savedReport = captureSavedReport()
            assertThat(savedReport.reportGroup).isSameAs(existingReportGroup)
            assertThat(existingReportGroup.reportCount).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("reportComment")
    inner class ReportComment {

        @Test
        @DisplayName("saves a pending comment report when the target is valid")
        fun reportComment_savesReport() {
            val request = ReportRequestDTO(COMMENT_ID, "ABUSE", "Insulting content")
            givenExistingReporter()
            givenExistingCommentByAnotherMember(deleted = false)
            givenDuplicateReportExists(TargetType.COMMENT, COMMENT_ID, exists = false)
            val reportGroup = givenNewReportGroup(TargetType.COMMENT, COMMENT_ID)

            userReportService.reportComment(REPORTER_ID, request)

            val savedReport = captureSavedReport()
            assertThat(savedReport.reporter).isSameAs(reporter)
            assertThat(savedReport.targetType).isEqualTo(TargetType.COMMENT)
            assertThat(savedReport.targetId).isEqualTo(COMMENT_ID)
            assertThat(savedReport.reasonType).isEqualTo("ABUSE")
            assertThat(savedReport.reasonDetail).isEqualTo("Insulting content")
            assertThat(savedReport.reportGroup).isSameAs(reportGroup)
            assertThat(reportGroup.reportCount).isEqualTo(1L)
        }

        @Test
        @DisplayName("throws when comment does not exist")
        fun reportComment_throwsWhenCommentMissing() {
            givenExistingReporter()
            whenever(commentRepository.findById(COMMENT_ID)).thenReturn(null.toOptional())

            assertReportError(
                action = { userReportService.reportComment(REPORTER_ID, ReportRequestDTO(COMMENT_ID, "ABUSE", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_404_TARGET
            )
            verify(reportRepository, never()).save(any())
        }

        @Test
        @DisplayName("throws when reporter owns the comment")
        fun reportComment_throwsWhenReportingOwnComment() {
            givenExistingReporter()
            whenever(comment.getUserId()).thenReturn(REPORTER_ID)
            whenever(commentRepository.findById(COMMENT_ID)).thenReturn(comment.toOptional())

            assertReportError(
                action = { userReportService.reportComment(REPORTER_ID, ReportRequestDTO(COMMENT_ID, "ABUSE", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_400_REPORT_SELF
            )
            verify(reportRepository, never()).save(any())
        }

        @Test
        @DisplayName("throws when comment is already deleted")
        fun reportComment_throwsWhenCommentDeleted() {
            givenExistingReporter()
            givenExistingCommentByAnotherMember(deleted = true)

            assertReportError(
                action = { userReportService.reportComment(REPORTER_ID, ReportRequestDTO(COMMENT_ID, "ABUSE", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_410_ALREADY_DELETED
            )
            verify(reportRepository, never()).save(any())
        }

        @Test
        @DisplayName("throws when reporter already reported the same comment")
        fun reportComment_throwsWhenDuplicate() {
            givenExistingReporter()
            givenExistingCommentByAnotherMember(deleted = false)
            givenDuplicateReportExists(TargetType.COMMENT, COMMENT_ID, exists = true)

            assertReportError(
                action = { userReportService.reportComment(REPORTER_ID, ReportRequestDTO(COMMENT_ID, "ABUSE", null)) },
                expectedErrorCode = ReportErrorCode.REPORT_409_ALREADY_REPORT_USER
            )
            verify(reportRepository, never()).save(any())
        }
    }

    private fun givenExistingReporter() {
        whenever(memberRepository.findById(REPORTER_ID)).thenReturn(reporter.toOptional())
    }

    private fun givenExistingPostByAnotherMember(deleted: Boolean) {
        whenever(post.member).thenReturn(author)
        whenever(post.isDeleted).thenReturn(deleted)
        whenever(postRepository.findById(POST_ID)).thenReturn(post.toOptional())
    }

    private fun givenExistingCommentByAnotherMember(deleted: Boolean) {
        whenever(comment.getUserId()).thenReturn(AUTHOR_ID)
        whenever(comment.isDeleted).thenReturn(deleted)
        whenever(commentRepository.findById(COMMENT_ID)).thenReturn(comment.toOptional())
    }

    private fun givenDuplicateReportExists(
        targetType: TargetType,
        targetId: Long,
        exists: Boolean
    ) {
        whenever(
            reportRepository.existsByReporterAndTargetTypeAndTargetId(
                reporter,
                targetType,
                targetId
            )
        ).thenReturn(exists)
    }

    private fun givenNewReportGroup(targetType: TargetType, targetId: Long): ReportGroup {
        val reportGroup = ReportGroup(targetType, targetId, LocalDateTime.now())

        whenever(reportGroupRepository.findByTargetTypeAndTargetId(targetType, targetId))
            .thenReturn(null, reportGroup)
        whenever(reportGroupCreationService.createOpenReportGroup(any(), any(), any()))
            .thenReturn(reportGroup)

        return reportGroup
    }

    private fun captureSavedReport(): Report {
        val captor = argumentCaptor<Report>()
        verify(reportRepository).save(captor.capture())
        return captor.firstValue
    }

    private fun assertReportError(
        action: () -> Unit,
        expectedErrorCode: Any
    ) {
        assertThatThrownBy(action)
            .isInstanceOf(ApiException::class.java)
            .extracting { error -> (error as ApiException).errorCode }
            .isEqualTo(expectedErrorCode)
    }

    private companion object {
        const val REPORTER_ID = 1L
        const val AUTHOR_ID = 2L
        const val POST_ID = 10L
        const val COMMENT_ID = 20L
    }
}
