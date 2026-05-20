package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.interaction.report.service.UserReportService
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCodeSpec
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.exception.errorCode.ReportErrorCode
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserReportService")
internal class UserReportServiceTest {
    @InjectMocks
    private val userReportService: UserReportService? = null

    @Mock
    private val reportRepository: ReportRepository? = null

    @Mock
    private val reportGroupRepository: ReportGroupRepository? = null

    @Mock
    private val memberRepository: MemberRepository? = null

    @Mock
    private val postRepository: PostRepository? = null

    @Mock
    private val commentRepository: CommentRepository? = null

    private var reporter: Member? = null
    private var author: Member? = null
    private var post: Post? = null
    private var comment: Comment? = null

    @BeforeEach
    fun setUp() {
        reporter = Mockito.mock<Member>(Member::class.java)
        author = Mockito.mock<Member>(Member::class.java)
        post = Mockito.mock<Post>(Post::class.java)
        comment = Mockito.mock<Comment>(Comment::class.java)

        BDDMockito.given<Long?>(reporter!!.userId).willReturn(1L)
        BDDMockito.given<Long?>(author!!.userId).willReturn(2L)
    }

    @Nested
    @DisplayName("reportPost")
    internal inner class ReportPost {
        @Test
        @DisplayName("saves a pending post report when the target is valid")
        fun reportPost_savesReport() {
            val dto = ReportRequestDTO(10L, "SPAM", "Repeated promotion")
            givenExistingReporter()
            givenExistingPostByAnotherMember(false)
            BDDMockito.given<Boolean?>(
                reportRepository!!.existsByReporterAndTargetTypeAndTargetId(
                    reporter!!,
                    TargetType.POST,
                    10L
                )
            )
                .willReturn(false)
            val reportGroup = givenNewReportGroup(TargetType.POST, 10L)

            userReportService!!.reportPost(1L, dto)

            val captor = ArgumentCaptor.forClass<Report?, Report?>(Report::class.java)
            Mockito.verify<ReportRepository?>(reportRepository).save<Report?>(captor.capture())
            val savedReport = captor.getValue()
            Assertions.assertThat<Member>(savedReport.reporter).isSameAs(reporter)
            Assertions.assertThat<TargetType>(savedReport.targetType).isEqualTo(TargetType.POST)
            Assertions.assertThat(savedReport.targetId).isEqualTo(10L)
            Assertions.assertThat(savedReport.reasonType).isEqualTo("SPAM")
            Assertions.assertThat(savedReport.reasonDetail).isEqualTo("Repeated promotion")
            Assertions.assertThat<ReportGroup?>(savedReport.reportGroup).isSameAs(reportGroup)
            Assertions.assertThat(reportGroup.reportCount).isEqualTo(1L)
        }

        @Test
        @DisplayName("throws when reporter does not exist")
        fun reportPost_throwsWhenReporterMissing() {
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L)).willReturn(Optional.empty<Member>())

            assertReportError(
                Runnable { userReportService!!.reportPost(1L, ReportRequestDTO(10L, "SPAM", null)) },
                MemberErrorCode.MEMBER_NOT_FOUND
            )
            Mockito.verifyNoInteractions(postRepository, commentRepository, reportRepository, reportGroupRepository)
        }

        @Test
        @DisplayName("throws when post does not exist")
        fun reportPost_throwsWhenPostMissing() {
            givenExistingReporter()
            BDDMockito.given<Optional<Post>?>(postRepository!!.findById(10L)).willReturn(Optional.empty<Post>())

            assertReportError(
                Runnable { userReportService!!.reportPost(1L, ReportRequestDTO(10L, "SPAM", null)) },
                ReportErrorCode.REPORT_404_TARGET
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }

        @Test
        @DisplayName("throws when reporter owns the post")
        fun reportPost_throwsWhenReportingOwnPost() {
            givenExistingReporter()
            BDDMockito.given<Member>(post!!.member).willReturn(reporter)
            BDDMockito.given<Optional<Post>?>(postRepository!!.findById(10L)).willReturn(Optional.of<Post>(post!!))

            assertReportError(
                Runnable { userReportService!!.reportPost(1L, ReportRequestDTO(10L, "SPAM", null)) },
                ReportErrorCode.REPORT_400_REPORT_SELF
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }

        @Test
        @DisplayName("throws when post is already deleted")
        fun reportPost_throwsWhenPostDeleted() {
            givenExistingReporter()
            givenExistingPostByAnotherMember(true)

            assertReportError(
                Runnable { userReportService!!.reportPost(1L, ReportRequestDTO(10L, "SPAM", null)) },
                ReportErrorCode.REPORT_410_ALREADY_DELETED
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }

        @Test
        @DisplayName("throws when reporter already reported the same post")
        fun reportPost_throwsWhenDuplicate() {
            givenExistingReporter()
            givenExistingPostByAnotherMember(false)
            BDDMockito.given<Boolean?>(
                reportRepository!!.existsByReporterAndTargetTypeAndTargetId(
                    reporter!!,
                    TargetType.POST,
                    10L
                )
            )
                .willReturn(true)

            assertReportError(
                Runnable { userReportService!!.reportPost(1L, ReportRequestDTO(10L, "SPAM", null)) },
                ReportErrorCode.REPORT_409_ALREADY_REPORT_USER
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }
    }

    @Nested
    @DisplayName("reportComment")
    internal inner class ReportComment {
        @Test
        @DisplayName("saves a pending comment report when the target is valid")
        fun reportComment_savesReport() {
            val dto = ReportRequestDTO(20L, "ABUSE", "Insulting content")
            givenExistingReporter()
            givenExistingCommentByAnotherMember(false)
            BDDMockito.given<Boolean?>(
                reportRepository!!.existsByReporterAndTargetTypeAndTargetId(
                    reporter!!,
                    TargetType.COMMENT,
                    20L
                )
            )
                .willReturn(false)
            val reportGroup = givenNewReportGroup(TargetType.COMMENT, 20L)

            userReportService!!.reportComment(1L, dto)

            val captor = ArgumentCaptor.forClass<Report?, Report?>(Report::class.java)
            Mockito.verify<ReportRepository?>(reportRepository).save<Report?>(captor.capture())
            val savedReport = captor.getValue()
            Assertions.assertThat<Member>(savedReport.reporter).isSameAs(reporter)
            Assertions.assertThat<TargetType>(savedReport.targetType).isEqualTo(TargetType.COMMENT)
            Assertions.assertThat(savedReport.targetId).isEqualTo(20L)
            Assertions.assertThat(savedReport.reasonType).isEqualTo("ABUSE")
            Assertions.assertThat(savedReport.reasonDetail).isEqualTo("Insulting content")
            Assertions.assertThat<ReportGroup?>(savedReport.reportGroup).isSameAs(reportGroup)
            Assertions.assertThat(reportGroup.reportCount).isEqualTo(1L)
        }

        @Test
        @DisplayName("throws when comment does not exist")
        fun reportComment_throwsWhenCommentMissing() {
            givenExistingReporter()
            BDDMockito.given<Optional<Comment>?>(commentRepository!!.findById(20L))
                .willReturn(Optional.empty<Comment>())

            assertReportError(
                Runnable { userReportService!!.reportComment(1L, ReportRequestDTO(20L, "ABUSE", null)) },
                ReportErrorCode.REPORT_404_TARGET
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }

        @Test
        @DisplayName("throws when reporter owns the comment")
        fun reportComment_throwsWhenReportingOwnComment() {
            givenExistingReporter()
            BDDMockito.given<Long?>(comment!!.getUserId()).willReturn(1L)
            BDDMockito.given<Optional<Comment>?>(commentRepository!!.findById(20L))
                .willReturn(Optional.of<Comment>(comment!!))

            assertReportError(
                Runnable { userReportService!!.reportComment(1L, ReportRequestDTO(20L, "ABUSE", null)) },
                ReportErrorCode.REPORT_400_REPORT_SELF
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }

        @Test
        @DisplayName("throws when comment is already deleted")
        fun reportComment_throwsWhenCommentDeleted() {
            givenExistingReporter()
            givenExistingCommentByAnotherMember(true)

            assertReportError(
                Runnable { userReportService!!.reportComment(1L, ReportRequestDTO(20L, "ABUSE", null)) },
                ReportErrorCode.REPORT_410_ALREADY_DELETED
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }

        @Test
        @DisplayName("throws when reporter already reported the same comment")
        fun reportComment_throwsWhenDuplicate() {
            givenExistingReporter()
            givenExistingCommentByAnotherMember(false)
            BDDMockito.given<Boolean?>(
                reportRepository!!.existsByReporterAndTargetTypeAndTargetId(
                    reporter!!,
                    TargetType.COMMENT,
                    20L
                )
            )
                .willReturn(true)

            assertReportError(
                Runnable { userReportService!!.reportComment(1L, ReportRequestDTO(20L, "ABUSE", null)) },
                ReportErrorCode.REPORT_409_ALREADY_REPORT_USER
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .save<Report>(ArgumentMatchers.any<Report>())
        }
    }

    private fun givenExistingReporter() {
        BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L)).willReturn(Optional.of<Member>(reporter!!))
    }

    private fun givenExistingPostByAnotherMember(deleted: Boolean) {
        BDDMockito.given<Member>(post!!.member).willReturn(author)
        BDDMockito.given<Boolean?>(post!!.isDeleted).willReturn(deleted)
        BDDMockito.given<Optional<Post>?>(postRepository!!.findById(10L)).willReturn(Optional.of<Post>(post!!))
    }

    private fun givenExistingCommentByAnotherMember(deleted: Boolean) {
        BDDMockito.given<Long?>(comment!!.getUserId()).willReturn(2L)
        BDDMockito.given<Boolean?>(comment!!.isDeleted).willReturn(deleted)
        BDDMockito.given<Optional<Comment>?>(commentRepository!!.findById(20L))
            .willReturn(Optional.of<Comment>(comment!!))
    }

    private fun givenNewReportGroup(targetType: TargetType, targetId: Long): ReportGroup {
        val reportGroup = ReportGroup(
            targetType,
            targetId,
            LocalDateTime.now()
        )

        BDDMockito.given<ReportGroup?>(reportGroupRepository!!.findByTargetTypeAndTargetId(targetType, targetId))
            .willReturn(null)
        BDDMockito.given<ReportGroup?>(
            reportGroupRepository.saveAndFlush<ReportGroup?>(
                ArgumentMatchers.any<ReportGroup?>(
                    ReportGroup::class.java
                )
            )
        )
            .willReturn(reportGroup)

        return reportGroup
    }

    private fun assertReportError(action: Runnable, expectedErrorCode: Any?) {
        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable { action.run() })
            .isInstanceOf(ApiException::class.java)
            .extracting<ErrorCodeSpec> { e: Throwable? -> (e as ApiException).errorCode }
            .isEqualTo(expectedErrorCode)
    }
}
