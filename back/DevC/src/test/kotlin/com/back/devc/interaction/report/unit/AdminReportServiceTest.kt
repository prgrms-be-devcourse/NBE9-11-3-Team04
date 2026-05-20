package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.AdminReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ApproveReportGroupRequest
import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.interaction.report.dto.RejectReportGroupRequest
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupAction
import com.back.devc.domain.interaction.report.entity.ReportGroupActionType
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.SanctionType
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupActionRepository
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportReasonStatProjection
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.interaction.report.service.AdminReportService
import com.back.devc.domain.interaction.report.util.ReportTargetHandler
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.entity.Comment
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.exception.ErrorCodeSpec
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.exception.errorCode.ReportErrorCode
import com.back.devc.interaction.report.setPrivateField
import com.back.devc.interaction.report.toOptional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AdminReportService")
internal class AdminReportServiceTest {
    private val reportRepository = mock<ReportRepository>()
    private val reportGroupRepository = mock<ReportGroupRepository>()
    private val reportGroupActionRepository = mock<ReportGroupActionRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val postRepository = mock<PostRepository>()
    private val commentRepository = mock<CommentRepository>()
    private val reportTargetHandler = mock<ReportTargetHandler>()

    private val adminReportService = AdminReportService(
        reportRepository = reportRepository,
        reportGroupRepository = reportGroupRepository,
        reportGroupActionRepository = reportGroupActionRepository,
        memberRepository = memberRepository,
        postRepository = postRepository,
        commentRepository = commentRepository,
        reportTargetHandler = reportTargetHandler
    )

    private lateinit var admin: Member
    private lateinit var user: Member

    @BeforeEach
    fun setUp() {
        admin = mock()
        user = mock()

        whenever(admin.isAdmin()).thenReturn(true)
        whenever(admin.userId).thenReturn(ADMIN_ID)
        whenever(user.isAdmin()).thenReturn(false)
    }

    @Nested
    @DisplayName("getReports")
    inner class GetReports {

        @Test
        @DisplayName("uses findAll when status is null")
        fun getReports_usesFindAllWithoutStatus() {
            val pageable = PageRequest.of(0, 10)
            val report = report(ReportStatus.PENDING)
            val dto = mock<ReportResponseDTO>()
            whenever(reportRepository.findAll(pageable)).thenReturn(PageImpl(listOf(report), pageable, 1))
            whenever(reportTargetHandler.toDtoWithTargetInfo(report)).thenReturn(dto)

            val result = adminReportService.getReports(null, pageable)

            assertThat(result.content).containsExactly(dto)
            verify(reportRepository).findAll(pageable)
            verify(reportRepository, never()).findAllByStatus(any(), any())
        }

        @Test
        @DisplayName("uses findAllByStatus when status is provided")
        fun getReports_usesFindAllByStatus() {
            val pageable = PageRequest.of(0, 10)
            val report = report(ReportStatus.PENDING)
            val dto = mock<ReportResponseDTO>()
            whenever(reportRepository.findAllByStatus(ReportStatus.PENDING, pageable))
                .thenReturn(PageImpl(listOf(report), pageable, 1))
            whenever(reportTargetHandler.toDtoWithTargetInfo(report)).thenReturn(dto)

            val result = adminReportService.getReports(ReportStatus.PENDING, pageable)

            assertThat(result.content).containsExactly(dto)
            verify(reportRepository).findAllByStatus(ReportStatus.PENDING, pageable)
            verify(reportRepository, never()).findAll(pageable)
        }
    }

    @Nested
    @DisplayName("getGroupedReports")
    inner class GetGroupedReports {

        @Test
        @DisplayName("maps report groups with batch-loaded target information")
        fun getGroupedReports_mapsRowsWithBatchTargetInfo() {
            val latestPostReport = LocalDateTime.of(2026, 1, 2, 10, 0)
            val latestCommentReport = LocalDateTime.of(2026, 1, 2, 11, 0)
            val from = LocalDateTime.of(2026, 1, 1, 0, 0)
            val to = LocalDateTime.of(2026, 1, 3, 0, 0)
            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "latestCreatedAt"))
            val reportGroupPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "latestReportedAt"))
            val postReportGroup = reportGroup(1L, TargetType.POST, POST_ID, 2, latestPostReport)
            val commentReportGroup = reportGroup(2L, TargetType.COMMENT, COMMENT_ID, 3, latestCommentReport)

            whenever(reportGroupRepository.findReportGroups(ReportGroupStatus.OPEN, from, to, reportGroupPageable))
                .thenReturn(PageImpl(listOf(postReportGroup, commentReportGroup), reportGroupPageable, 2))

            val postAuthor = mock<Member>()
            val post = mock<Post>()
            whenever(postAuthor.nickname).thenReturn("post-writer")
            whenever(post.postId).thenReturn(POST_ID)
            whenever(post.member).thenReturn(postAuthor)
            whenever(post.title).thenReturn("reported post")
            whenever(post.content).thenReturn("post content")
            whenever(postRepository.findAllByPostIdIn(listOf(POST_ID))).thenReturn(listOf(post))

            val comment = mock<Comment>()
            whenever(comment.id).thenReturn(COMMENT_ID)
            whenever(comment.getUserId()).thenReturn(COMMENT_AUTHOR_ID)
            whenever(comment.content).thenReturn("comment content")
            whenever(commentRepository.findAllByIdIn(listOf(COMMENT_ID))).thenReturn(listOf(comment))

            val commentAuthor = mock<Member>()
            whenever(commentAuthor.userId).thenReturn(COMMENT_AUTHOR_ID)
            whenever(commentAuthor.nickname).thenReturn("comment-writer")
            whenever(memberRepository.findAllById(listOf(COMMENT_AUTHOR_ID))).thenReturn(listOf(commentAuthor))

            whenever(reportRepository.findReasonStatsByReportGroupIds(listOf(1L, 2L)))
                .thenReturn(
                    listOf(
                        reasonStat(1L, "SPAM", 1L),
                        reasonStat(2L, "ABUSE", 1L),
                        reasonStat(2L, "HATE", 1L)
                    )
                )

            val result = adminReportService.getGroupedReports(ReportStatus.PENDING, from, to, pageable)

            assertThat(result.content).hasSize(2)

            val postGroup = result.content[0]
            assertThat(postGroup.targetType).isEqualTo(TargetType.POST)
            assertThat(postGroup.targetId).isEqualTo(POST_ID)
            assertThat(postGroup.targetNickname).isEqualTo("post-writer")
            assertThat(postGroup.targetTitle).isEqualTo("reported post")
            assertThat(postGroup.targetContent).isEqualTo("post content")
            assertThat(postGroup.reportCount).isEqualTo(2L)
            assertThat(postGroup.reasonTypes).containsExactly("SPAM")

            val commentGroup = result.content[1]
            assertThat(commentGroup.targetType).isEqualTo(TargetType.COMMENT)
            assertThat(commentGroup.targetId).isEqualTo(COMMENT_ID)
            assertThat(commentGroup.targetNickname).isEqualTo("comment-writer")
            assertThat(commentGroup.targetTitle).isNull()
            assertThat(commentGroup.targetContent).isEqualTo("comment content")
            assertThat(commentGroup.reportCount).isEqualTo(3L)
            assertThat(commentGroup.reasonTypes).containsExactly("ABUSE", "HATE")
        }

        @Test
        @DisplayName("rejects inverted date range")
        fun getGroupedReports_rejectsInvalidDateRange() {
            val from = LocalDateTime.of(2026, 2, 1, 0, 0)
            val to = LocalDateTime.of(2026, 1, 1, 0, 0)

            assertServiceError(
                action = { adminReportService.getGroupedReports(null, from, to, PageRequest.of(0, 10)) },
                expectedErrorCode = ErrorCode.BAD_REQUEST
            )
        }

        @Test
        @DisplayName("rejects date ranges longer than ninety days")
        fun getGroupedReports_rejectsTooWideDateRange() {
            val from = LocalDateTime.of(2026, 1, 1, 0, 0)
            val to = LocalDateTime.of(2026, 4, 2, 0, 0)

            assertServiceError(
                action = { adminReportService.getGroupedReports(null, from, to, PageRequest.of(0, 10)) },
                expectedErrorCode = ErrorCode.BAD_REQUEST
            )
        }

        @Test
        @DisplayName("rejects page size over one hundred")
        fun getGroupedReports_rejectsTooLargePageSize() {
            assertServiceError(
                action = {
                    adminReportService.getGroupedReports(
                        null,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 2, 0, 0),
                        PageRequest.of(0, 101)
                    )
                },
                expectedErrorCode = ErrorCode.BAD_REQUEST
            )
        }

        @Test
        @DisplayName("rejects unsupported sort")
        fun getGroupedReports_rejectsUnsupportedSort() {
            assertServiceError(
                action = {
                    adminReportService.getGroupedReports(
                        null,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 2, 0, 0),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
                    )
                },
                expectedErrorCode = ErrorCode.BAD_REQUEST
            )
        }
    }

    @Nested
    @DisplayName("approveReport")
    inner class ApproveReport {

        @Test
        @DisplayName("processes a pending report and delegates target handling")
        fun approveReport_success() {
            val report = report(ReportStatus.PENDING)
            givenAdmin()
            whenever(reportRepository.findById(REPORT_ID)).thenReturn(report.toOptional())
            whenever(reportTargetHandler.exists(TargetType.POST, POST_ID)).thenReturn(true)

            adminReportService.approveReport(ADMIN_ID, approveDto())

            verify(report).processReport(admin)
            verify(reportTargetHandler).handleApproved(TargetType.POST, POST_ID, admin, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when admin member does not exist")
        fun approveReport_throwsWhenAdminMissing() {
            whenever(memberRepository.findById(ADMIN_ID)).thenReturn(null.toOptional())

            assertServiceError(
                action = { adminReportService.approveReport(ADMIN_ID, approveDto()) },
                expectedErrorCode = MemberErrorCode.MEMBER_NOT_FOUND
            )
            verifyNoInteractions(reportRepository, reportTargetHandler)
        }

        @Test
        @DisplayName("throws when member is not an admin")
        fun approveReport_throwsWhenNotAdmin() {
            whenever(memberRepository.findById(ADMIN_ID)).thenReturn(user.toOptional())

            assertServiceError(
                action = { adminReportService.approveReport(ADMIN_ID, approveDto()) },
                expectedErrorCode = ReportErrorCode.REPORT_403_UNAUTHORIZED_ADMIN
            )
            verifyNoInteractions(reportRepository, reportTargetHandler)
        }

        @Test
        @DisplayName("throws when report does not exist")
        fun approveReport_throwsWhenReportMissing() {
            givenAdmin()
            whenever(reportRepository.findById(REPORT_ID)).thenReturn(null.toOptional())

            assertServiceError(
                action = { adminReportService.approveReport(ADMIN_ID, approveDto()) },
                expectedErrorCode = ReportErrorCode.REPORT_404_REPORT
            )
        }

        @Test
        @DisplayName("throws when report is already processed")
        fun approveReport_throwsWhenAlreadyProcessed() {
            val report = report(ReportStatus.RESOLVED)
            givenAdmin()
            whenever(reportRepository.findById(REPORT_ID)).thenReturn(report.toOptional())

            assertServiceError(
                action = { adminReportService.approveReport(ADMIN_ID, approveDto()) },
                expectedErrorCode = ReportErrorCode.REPORT_409_ALREADY_REPORT
            )
            verify(reportTargetHandler, never())
                .handleApproved(TargetType.POST, POST_ID, admin, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when target does not exist")
        fun approveReport_throwsWhenTargetMissing() {
            val report = report(ReportStatus.PENDING)
            givenAdmin()
            whenever(reportRepository.findById(REPORT_ID)).thenReturn(report.toOptional())
            whenever(reportTargetHandler.exists(TargetType.POST, POST_ID)).thenReturn(false)

            assertServiceError(
                action = { adminReportService.approveReport(ADMIN_ID, approveDto()) },
                expectedErrorCode = ReportErrorCode.REPORT_404_TARGET
            )
        }

        @Test
        @DisplayName("throws when suspension has no positive day count")
        fun approveReport_throwsWhenSuspensionDaysInvalid() {
            val report = report(ReportStatus.PENDING)
            val dto = AdminReportRequestDTO(REPORT_ID, TargetType.POST, "note", SanctionType.SUSPENDED, 0)
            givenAdmin()
            whenever(reportRepository.findById(REPORT_ID)).thenReturn(report.toOptional())
            whenever(reportTargetHandler.exists(TargetType.POST, POST_ID)).thenReturn(true)

            assertServiceError(
                action = { adminReportService.approveReport(ADMIN_ID, dto) },
                expectedErrorCode = ReportErrorCode.REPORT_400_INVALID_SANCTION_PARAMETER
            )
            verify(report, never()).processReport(admin)
            verify(reportTargetHandler, never())
                .handleApproved(TargetType.POST, POST_ID, admin, SanctionType.SUSPENDED, 0)
        }
    }

    @Nested
    @DisplayName("rejectReport")
    inner class RejectReport {

        @Test
        @DisplayName("rejects a pending report and delegates target handling")
        fun rejectReport_success() {
            val report = report(ReportStatus.PENDING)
            givenAdmin()
            whenever(reportRepository.findById(REPORT_ID)).thenReturn(report.toOptional())

            adminReportService.rejectReport(ADMIN_ID, approveDto())

            verify(report).rejectReport(admin)
            verify(reportTargetHandler).handleRejected(TargetType.POST, POST_ID, admin)
        }

        @Test
        @DisplayName("throws when report is already processed")
        fun rejectReport_throwsWhenAlreadyProcessed() {
            val report = report(ReportStatus.REJECTED)
            givenAdmin()
            whenever(reportRepository.findById(REPORT_ID)).thenReturn(report.toOptional())

            assertServiceError(
                action = { adminReportService.rejectReport(ADMIN_ID, approveDto()) },
                expectedErrorCode = ReportErrorCode.REPORT_409_ALREADY_REPORT
            )
            verify(reportTargetHandler, never()).handleRejected(TargetType.POST, POST_ID, admin)
        }
    }

    @Nested
    @DisplayName("legacy approveReportGroup")
    inner class ApproveReportGroup {

        @Test
        @DisplayName("bulk resolves pending reports and delegates target handling")
        fun approveReportGroup_success() {
            val dto = AdminReportRequestDTO(POST_ID, TargetType.POST, "note", SanctionType.WARNED, null)
            givenAdmin()
            whenever(reportTargetHandler.exists(TargetType.POST, POST_ID)).thenReturn(true)
            whenever(
                reportRepository.updateStatusGroup(
                    TargetType.POST,
                    POST_ID,
                    admin,
                    ReportStatus.RESOLVED,
                    ReportStatus.PENDING
                )
            ).thenReturn(2)

            adminReportService.approveReportGroup(ADMIN_ID, dto)

            verify(reportRepository)
                .updateStatusGroup(TargetType.POST, POST_ID, admin, ReportStatus.RESOLVED, ReportStatus.PENDING)
            verify(reportTargetHandler).handleApproved(TargetType.POST, POST_ID, admin, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when there are no pending reports to approve")
        fun approveReportGroup_throwsWhenNoPendingReports() {
            val dto = AdminReportRequestDTO(POST_ID, TargetType.POST, "note", SanctionType.WARNED, null)
            givenAdmin()
            whenever(reportTargetHandler.exists(TargetType.POST, POST_ID)).thenReturn(true)
            whenever(
                reportRepository.updateStatusGroup(
                    TargetType.POST,
                    POST_ID,
                    admin,
                    ReportStatus.RESOLVED,
                    ReportStatus.PENDING
                )
            ).thenReturn(0)

            assertServiceError(
                action = { adminReportService.approveReportGroup(ADMIN_ID, dto) },
                expectedErrorCode = ReportErrorCode.REPORT_404_PENDING_LIST
            )
            verify(reportTargetHandler, never())
                .handleApproved(TargetType.POST, POST_ID, admin, SanctionType.WARNED, null)
        }
    }

    @Nested
    @DisplayName("legacy rejectReportGroup")
    inner class RejectReportGroup {

        @Test
        @DisplayName("bulk rejects pending reports and delegates target handling")
        fun rejectReportGroup_success() {
            val dto = AdminReportRequestDTO(POST_ID, TargetType.POST, "note", null, null)
            givenAdmin()
            whenever(
                reportRepository.updateStatusGroup(
                    TargetType.POST,
                    POST_ID,
                    admin,
                    ReportStatus.REJECTED,
                    ReportStatus.PENDING
                )
            ).thenReturn(1)

            adminReportService.rejectReportGroup(ADMIN_ID, dto)

            verify(reportRepository)
                .updateStatusGroup(TargetType.POST, POST_ID, admin, ReportStatus.REJECTED, ReportStatus.PENDING)
            verify(reportTargetHandler).handleRejected(TargetType.POST, POST_ID, admin)
        }

        @Test
        @DisplayName("throws when there are no pending reports to reject")
        fun rejectReportGroup_throwsWhenNoPendingReports() {
            val dto = AdminReportRequestDTO(POST_ID, TargetType.POST, "note", null, null)
            givenAdmin()
            whenever(
                reportRepository.updateStatusGroup(
                    TargetType.POST,
                    POST_ID,
                    admin,
                    ReportStatus.REJECTED,
                    ReportStatus.PENDING
                )
            ).thenReturn(0)

            assertServiceError(
                action = { adminReportService.rejectReportGroup(ADMIN_ID, dto) },
                expectedErrorCode = ReportErrorCode.REPORT_404_PENDING_LIST
            )
            verify(reportTargetHandler, never()).handleRejected(TargetType.POST, POST_ID, admin)
        }
    }

    @Nested
    @DisplayName("approveReportGroupById")
    inner class ApproveReportGroupById {

        @Test
        @DisplayName("approves report group and resolves pending reports during transition")
        fun approveReportGroupById_success() {
            val reportGroup = reportGroup()
            val request = ApproveReportGroupRequest("note", SanctionType.WARNED, null)
            givenAdmin()
            whenever(reportGroupRepository.findById(REPORT_GROUP_ID)).thenReturn(reportGroup.toOptional())
            whenever(reportTargetHandler.exists(TargetType.POST, POST_ID)).thenReturn(true)
            whenever(
                reportRepository.updateStatusByReportGroupId(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(2)

            adminReportService.approveReportGroupById(ADMIN_ID, REPORT_GROUP_ID, request)

            assertThat(reportGroup.status).isEqualTo(ReportGroupStatus.APPROVED)
            verify(reportGroupRepository).saveAndFlush(reportGroup)

            val actionCaptor = argumentCaptor<ReportGroupAction>()
            verify(reportGroupActionRepository).save(actionCaptor.capture())
            assertThat(actionCaptor.firstValue.actionType).isEqualTo(ReportGroupActionType.APPROVE)
            assertThat(actionCaptor.firstValue.beforeStatus).isEqualTo(ReportGroupStatus.OPEN)
            assertThat(actionCaptor.firstValue.afterStatus).isEqualTo(ReportGroupStatus.APPROVED)
            assertThat(actionCaptor.firstValue.note).isEqualTo("note")
            assertThat(actionCaptor.firstValue.sanctionType).isEqualTo(SanctionType.WARNED)
            assertThat(actionCaptor.firstValue.suspensionDays).isNull()

            val processedAtCaptor = argumentCaptor<LocalDateTime>()
            verify(reportRepository)
                .updateStatusByReportGroupId(
                    eq(REPORT_GROUP_ID),
                    eq(admin),
                    eq(ReportStatus.RESOLVED),
                    eq(ReportStatus.PENDING),
                    processedAtCaptor.capture()
                )
            assertThat(processedAtCaptor.firstValue).isEqualTo(reportGroup.processedAt)
            assertThat(processedAtCaptor.firstValue).isEqualTo(actionCaptor.firstValue.createdAt)
            verify(reportTargetHandler).handleApproved(TargetType.POST, POST_ID, admin, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when report group does not exist")
        fun approveReportGroupById_throwsWhenGroupMissing() {
            val request = ApproveReportGroupRequest("note", SanctionType.WARNED, null)
            givenAdmin()
            whenever(reportGroupRepository.findById(REPORT_GROUP_ID)).thenReturn(null.toOptional())

            assertServiceError(
                action = { adminReportService.approveReportGroupById(ADMIN_ID, REPORT_GROUP_ID, request) },
                expectedErrorCode = ReportErrorCode.REPORT_404_REPORT_GROUP
            )
            verify(reportRepository, never()).updateStatusByReportGroupId(any(), any(), any(), any(), any())
            verifyNoInteractions(reportTargetHandler)
        }
    }

    @Nested
    @DisplayName("rejectReportGroupById")
    inner class RejectReportGroupById {

        @Test
        @DisplayName("rejects report group and rejects pending reports during transition")
        fun rejectReportGroupById_success() {
            val reportGroup = reportGroup()
            val request = RejectReportGroupRequest("not enough evidence")
            givenAdmin()
            whenever(reportGroupRepository.findById(REPORT_GROUP_ID)).thenReturn(reportGroup.toOptional())
            whenever(
                reportRepository.updateStatusByReportGroupId(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(2)

            adminReportService.rejectReportGroupById(ADMIN_ID, REPORT_GROUP_ID, request)

            assertThat(reportGroup.status).isEqualTo(ReportGroupStatus.REJECTED)
            verify(reportGroupRepository).saveAndFlush(reportGroup)

            val actionCaptor = argumentCaptor<ReportGroupAction>()
            verify(reportGroupActionRepository).save(actionCaptor.capture())
            assertThat(actionCaptor.firstValue.actionType).isEqualTo(ReportGroupActionType.REJECT)
            assertThat(actionCaptor.firstValue.beforeStatus).isEqualTo(ReportGroupStatus.OPEN)
            assertThat(actionCaptor.firstValue.afterStatus).isEqualTo(ReportGroupStatus.REJECTED)
            assertThat(actionCaptor.firstValue.note).isEqualTo("not enough evidence")
            assertThat(actionCaptor.firstValue.sanctionType).isNull()
            assertThat(actionCaptor.firstValue.suspensionDays).isNull()

            val processedAtCaptor = argumentCaptor<LocalDateTime>()
            verify(reportRepository)
                .updateStatusByReportGroupId(
                    eq(REPORT_GROUP_ID),
                    eq(admin),
                    eq(ReportStatus.REJECTED),
                    eq(ReportStatus.PENDING),
                    processedAtCaptor.capture()
                )
            assertThat(processedAtCaptor.firstValue).isEqualTo(reportGroup.processedAt)
            assertThat(processedAtCaptor.firstValue).isEqualTo(actionCaptor.firstValue.createdAt)
            verify(reportTargetHandler).handleRejected(TargetType.POST, POST_ID, admin)
        }
    }

    private fun givenAdmin() {
        whenever(memberRepository.findById(ADMIN_ID)).thenReturn(admin.toOptional())
    }

    private fun reportGroup(): ReportGroup =
        reportGroup(REPORT_GROUP_ID, TargetType.POST, POST_ID, reportCount = 1, latestReportedAt = NOW)

    private fun reportGroup(
        reportGroupId: Long,
        targetType: TargetType,
        targetId: Long,
        reportCount: Int,
        latestReportedAt: LocalDateTime
    ): ReportGroup {
        val reportGroup = ReportGroup(targetType, targetId, latestReportedAt)
        repeat(reportCount) {
            reportGroup.registerReport(latestReportedAt)
        }
        reportGroup.setPrivateField("reportGroupId", reportGroupId)
        return reportGroup
    }

    private fun reasonStat(
        reportGroupId: Long,
        reasonType: String,
        reasonCount: Long
    ): ReportReasonStatProjection =
        object : ReportReasonStatProjection {
            override val reportGroupId = reportGroupId
            override val reasonType = reasonType
            override val reasonCount = reasonCount
        }

    private fun report(status: ReportStatus): Report {
        val report = mock<Report>()
        whenever(report.reportId).thenReturn(REPORT_ID)
        whenever(report.status).thenReturn(status)
        whenever(report.targetType).thenReturn(TargetType.POST)
        whenever(report.targetId).thenReturn(POST_ID)
        return report
    }

    private fun approveDto(): AdminReportRequestDTO =
        AdminReportRequestDTO(REPORT_ID, TargetType.POST, "note", SanctionType.WARNED, null)

    private fun assertServiceError(action: () -> Unit, expectedErrorCode: ErrorCodeSpec) {
        assertThatThrownBy(action)
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).errorCode }
            .isEqualTo(expectedErrorCode)
    }

    private companion object {
        const val ADMIN_ID = 1L
        const val REPORT_ID = 100L
        const val REPORT_GROUP_ID = 10L
        const val POST_ID = 10L
        const val COMMENT_ID = 20L
        const val COMMENT_AUTHOR_ID = 30L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)
    }
}
