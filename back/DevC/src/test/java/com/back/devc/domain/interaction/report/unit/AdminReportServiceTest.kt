package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.*
import com.back.devc.domain.interaction.report.entity.*
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
import org.springframework.data.domain.*
import java.time.LocalDateTime
import java.util.*
import java.util.List

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminReportService")
internal class AdminReportServiceTest {
    @InjectMocks
    private val adminReportService: AdminReportService? = null

    @Mock
    private val reportRepository: ReportRepository? = null

    @Mock
    private val reportGroupRepository: ReportGroupRepository? = null

    @Mock
    private val reportGroupActionRepository: ReportGroupActionRepository? = null

    @Mock
    private val memberRepository: MemberRepository? = null

    @Mock
    private val postRepository: PostRepository? = null

    @Mock
    private val commentRepository: CommentRepository? = null

    @Mock
    private val reportTargetHandler: ReportTargetHandler? = null

    private var admin: Member? = null
    private var user: Member? = null

    @BeforeEach
    fun setUp() {
        admin = Mockito.mock<Member>(Member::class.java)
        user = Mockito.mock<Member>(Member::class.java)

        BDDMockito.given<Boolean?>(admin!!.isAdmin()).willReturn(true)
        BDDMockito.given<Long?>(admin!!.userId).willReturn(1L)
        BDDMockito.given<Boolean?>(user!!.isAdmin()).willReturn(false)
    }

    @Nested
    @DisplayName("getReports")
    internal inner class GetReports {
        @Test
        @DisplayName("uses findAll when status is null")
        fun getReports_usesFindAllWithoutStatus() {
            val pageable: Pageable = PageRequest.of(0, 10)
            val report = report(ReportStatus.PENDING)
            val dto = Mockito.mock<ReportResponseDTO?>(ReportResponseDTO::class.java)
            BDDMockito.given<Page<Report>?>(reportRepository!!.findAll(pageable))
                .willReturn(PageImpl<Report?>(List.of<Report>(report), pageable, 1))
            BDDMockito.given<ReportResponseDTO>(reportTargetHandler!!.toDtoWithTargetInfo(report)).willReturn(dto)

            val result: Page<ReportResponseDTO?> = adminReportService!!.getReports(null, pageable)

            Assertions.assertThat<ReportResponseDTO>(result.getContent()).containsExactly(dto)
            Mockito.verify<ReportRepository?>(reportRepository).findAll(pageable)
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never())
                .findAllByStatus(ArgumentMatchers.any<ReportStatus>(), ArgumentMatchers.any<Pageable>())
        }

        @Test
        @DisplayName("uses findAllByStatus when status is provided")
        fun getReports_usesFindAllByStatus() {
            val pageable: Pageable = PageRequest.of(0, 10)
            val report = report(ReportStatus.PENDING)
            val dto = Mockito.mock<ReportResponseDTO?>(ReportResponseDTO::class.java)
            BDDMockito.given<Page<Report>>(reportRepository!!.findAllByStatus(ReportStatus.PENDING, pageable))
                .willReturn(PageImpl<Report?>(List.of<Report>(report), pageable, 1))
            BDDMockito.given<ReportResponseDTO>(reportTargetHandler!!.toDtoWithTargetInfo(report)).willReturn(dto)

            val result: Page<ReportResponseDTO?> = adminReportService!!.getReports(ReportStatus.PENDING, pageable)

            Assertions.assertThat<ReportResponseDTO>(result.getContent()).containsExactly(dto)
            Mockito.verify<ReportRepository?>(reportRepository).findAllByStatus(ReportStatus.PENDING, pageable)
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never()).findAll(pageable)
        }
    }

    @Nested
    @DisplayName("getGroupedReports")
    internal inner class GetGroupedReports {
        @Test
        @DisplayName("maps grouped post and comment rows with batch-loaded target information")
        fun getGroupedReports_mapsRowsWithBatchTargetInfo() {
            val latestPostReport = LocalDateTime.of(2026, 1, 2, 10, 0)
            val latestCommentReport = LocalDateTime.of(2026, 1, 2, 11, 0)
            val from = LocalDateTime.of(2026, 1, 1, 0, 0)
            val to = LocalDateTime.of(2026, 1, 3, 0, 0)
            val pageable: Pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "latestCreatedAt"))

            val reportGroupPageable: Pageable =
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "latestReportedAt"))
            val postReportGroup =
                reportGroup(1L, TargetType.POST, 10L, 2, latestPostReport)
            val commentReportGroup =
                reportGroup(2L, TargetType.COMMENT, 20L, 3, latestCommentReport)

            BDDMockito.given<Page<ReportGroup>>(
                reportGroupRepository!!.findReportGroups(
                    ReportGroupStatus.OPEN,
                    from,
                    to,
                    reportGroupPageable
                )
            ).willReturn(
                PageImpl<ReportGroup?>(
                    List.of<ReportGroup>(postReportGroup, commentReportGroup),
                    reportGroupPageable,
                    2
                )
            )

            val postAuthor = Mockito.mock<Member>(Member::class.java)
            BDDMockito.given<String>(postAuthor.nickname).willReturn("post-writer")
            val post = Mockito.mock<Post>(Post::class.java)
            BDDMockito.given<Long?>(post.postId).willReturn(10L)
            BDDMockito.given<Member>(post.member).willReturn(postAuthor)
            BDDMockito.given<String>(post.title).willReturn("reported post")
            BDDMockito.given<String>(post.content).willReturn("post content")
            BDDMockito.given<MutableList<Post>>(postRepository!!.findAllByPostIdIn(mutableListOf<Long>(10L)))
                .willReturn(
                    List.of<Post>(post)
                )

            val comment = Mockito.mock<Comment>(Comment::class.java)
            BDDMockito.given<Long?>(comment.id).willReturn(20L)
            BDDMockito.given<Long?>(comment.getUserId()).willReturn(30L)
            BDDMockito.given<String?>(comment.content).willReturn("comment content")
            BDDMockito.given<MutableList<Comment>>(commentRepository!!.findAllByIdIn(mutableListOf<Long>(20L)))
                .willReturn(
                    List.of<Comment>(comment)
                )

            val commentAuthor = Mockito.mock<Member>(Member::class.java)
            BDDMockito.given<Long?>(commentAuthor.userId).willReturn(30L)
            BDDMockito.given<String>(commentAuthor.nickname).willReturn("comment-writer")

            // 🔧 고쳐야 할 부분: findAllByUserIdIn → findAllById
            BDDMockito.given<MutableList<Member>?>(memberRepository!!.findAllById(mutableListOf<Long>(30L)))
                .willReturn(List.of<Member>(commentAuthor))

            BDDMockito.given<MutableList<ReportReasonStatProjection>>(
                reportRepository!!.findReasonStatsByReportGroupIds(
                    mutableListOf<Long>(1L, 2L)
                )
            )
                .willReturn(
                    List.of<ReportReasonStatProjection>(
                        reasonStat(1L, "SPAM", 1L),
                        reasonStat(2L, "ABUSE", 1L),
                        reasonStat(2L, "HATE", 1L)
                    )
                )

            val result = adminReportService!!.getGroupedReports(ReportStatus.PENDING, from, to, pageable)

            Assertions.assertThat<ReportGroupResponseDTO>(result.getContent()).hasSize(2)
            val postGroup = result.getContent().get(0)
            Assertions.assertThat<TargetType>(postGroup.targetType).isEqualTo(TargetType.POST)
            Assertions.assertThat(postGroup.targetId).isEqualTo(10L)
            Assertions.assertThat(postGroup.targetNickname).isEqualTo("post-writer")
            Assertions.assertThat(postGroup.targetTitle).isEqualTo("reported post")
            Assertions.assertThat(postGroup.targetContent).isEqualTo("post content")
            Assertions.assertThat(postGroup.reportCount).isEqualTo(2L)
            Assertions.assertThat<String>(postGroup.reasonTypes).containsExactly("SPAM")

            val commentGroup = result.getContent().get(1)
            Assertions.assertThat<TargetType>(commentGroup.targetType).isEqualTo(TargetType.COMMENT)
            Assertions.assertThat(commentGroup.targetId).isEqualTo(20L)
            Assertions.assertThat(commentGroup.targetNickname).isEqualTo("comment-writer")
            Assertions.assertThat(commentGroup.targetTitle).isNull()
            Assertions.assertThat(commentGroup.targetContent).isEqualTo("comment content")
            Assertions.assertThat(commentGroup.reportCount).isEqualTo(3L)
            Assertions.assertThat<String>(commentGroup.reasonTypes).containsExactly("ABUSE", "HATE")
        }

        @Test
        @DisplayName("rejects null or inverted date range")
        fun getGroupedReports_rejectsInvalidDateRange() {
            val pageable: Pageable = PageRequest.of(0, 10)
            val from = LocalDateTime.of(2026, 2, 1, 0, 0)
            val to = LocalDateTime.of(2026, 1, 1, 0, 0)

            assertServiceError(
                Runnable { adminReportService!!.getGroupedReports(null, from, to, pageable) },
                ErrorCode.BAD_REQUEST
            )
        }

        @Test
        @DisplayName("rejects date ranges longer than ninety days")
        fun getGroupedReports_rejectsTooWideDateRange() {
            val pageable: Pageable = PageRequest.of(0, 10)
            val from = LocalDateTime.of(2026, 1, 1, 0, 0)
            val to = LocalDateTime.of(2026, 4, 2, 0, 0)

            assertServiceError(
                Runnable { adminReportService!!.getGroupedReports(null, from, to, pageable) },
                ErrorCode.BAD_REQUEST
            )
        }

        @Test
        @DisplayName("rejects page size over one hundred")
        fun getGroupedReports_rejectsTooLargePageSize() {
            val pageable: Pageable = PageRequest.of(0, 101)
            val from = LocalDateTime.of(2026, 1, 1, 0, 0)
            val to = LocalDateTime.of(2026, 1, 2, 0, 0)

            assertServiceError(
                Runnable { adminReportService!!.getGroupedReports(null, from, to, pageable) },
                ErrorCode.BAD_REQUEST
            )
        }

        @Test
        @DisplayName("rejects unsupported sort")
        fun getGroupedReports_rejectsUnsupportedSort() {
            val pageable: Pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
            val from = LocalDateTime.of(2026, 1, 1, 0, 0)
            val to = LocalDateTime.of(2026, 1, 2, 0, 0)

            assertServiceError(
                Runnable { adminReportService!!.getGroupedReports(null, from, to, pageable) },
                ErrorCode.BAD_REQUEST
            )
        }
    }

    @Nested
    @DisplayName("approveReport")
    internal inner class ApproveReport {
        @Test
        @DisplayName("processes a pending report and delegates target handling")
        fun approveReport_success() {
            val report = report(ReportStatus.PENDING)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<Report>?>(reportRepository!!.findById(100L))
                .willReturn(Optional.of<Report>(report))
            BDDMockito.given<Boolean?>(reportTargetHandler!!.exists(TargetType.POST, 10L)).willReturn(true)
            val dto = AdminReportRequestDTO(100L, TargetType.POST, "note", SanctionType.WARNED, null)

            adminReportService!!.approveReport(1L, dto)

            Mockito.verify<Report?>(report).processReport(admin!!)
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler)
                .handleApproved(TargetType.POST, 10L, admin!!, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when admin member does not exist")
        fun approveReport_throwsWhenAdminMissing() {
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L)).willReturn(Optional.empty<Member>())

            assertServiceError(
                Runnable { adminReportService!!.approveReport(1L, approveDto()) },
                MemberErrorCode.MEMBER_NOT_FOUND
            )
            Mockito.verifyNoInteractions(reportRepository, reportTargetHandler)
        }

        @Test
        @DisplayName("throws when member is not an admin")
        fun approveReport_throwsWhenNotAdmin() {
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L)).willReturn(Optional.of<Member>(user!!))

            assertServiceError(
                Runnable { adminReportService!!.approveReport(1L, approveDto()) },
                ReportErrorCode.REPORT_403_UNAUTHORIZED_ADMIN
            )
            Mockito.verifyNoInteractions(reportRepository, reportTargetHandler)
        }

        @Test
        @DisplayName("throws when report does not exist")
        fun approveReport_throwsWhenReportMissing() {
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<Report>?>(reportRepository!!.findById(100L)).willReturn(Optional.empty<Report>())

            assertServiceError(
                Runnable { adminReportService!!.approveReport(1L, approveDto()) },
                ReportErrorCode.REPORT_404_REPORT
            )
        }

        @Test
        @DisplayName("throws when report is already processed")
        fun approveReport_throwsWhenAlreadyProcessed() {
            val report = report(ReportStatus.RESOLVED)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<Report>?>(reportRepository!!.findById(100L))
                .willReturn(Optional.of<Report>(report))

            assertServiceError(
                Runnable { adminReportService!!.approveReport(1L, approveDto()) },
                ReportErrorCode.REPORT_409_ALREADY_REPORT
            )
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler, Mockito.never())
                .handleApproved(TargetType.POST, 10L, admin!!, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when target does not exist")
        fun approveReport_throwsWhenTargetMissing() {
            val report = report(ReportStatus.PENDING)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<Report>?>(reportRepository!!.findById(100L))
                .willReturn(Optional.of<Report>(report))
            BDDMockito.given<Boolean?>(reportTargetHandler!!.exists(TargetType.POST, 10L)).willReturn(false)

            assertServiceError(
                Runnable { adminReportService!!.approveReport(1L, approveDto()) },
                ReportErrorCode.REPORT_404_TARGET
            )
        }

        @Test
        @DisplayName("throws when suspension has no positive day count")
        fun approveReport_throwsWhenSuspensionDaysInvalid() {
            val report = report(ReportStatus.PENDING)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<Report>?>(reportRepository!!.findById(100L))
                .willReturn(Optional.of<Report>(report))
            BDDMockito.given<Boolean?>(reportTargetHandler!!.exists(TargetType.POST, 10L)).willReturn(true)
            val dto = AdminReportRequestDTO(100L, TargetType.POST, "note", SanctionType.SUSPENDED, 0)

            assertServiceError(
                Runnable { adminReportService!!.approveReport(1L, dto) },
                ReportErrorCode.REPORT_400_INVALID_SANCTION_PARAMETER
            )
            Mockito.verify<Report?>(report, Mockito.never()).processReport(admin!!)
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler, Mockito.never())
                .handleApproved(TargetType.POST, 10L, admin!!, SanctionType.SUSPENDED, 0)
        }
    }

    @Nested
    @DisplayName("rejectReport")
    internal inner class RejectReport {
        @Test
        @DisplayName("rejects a pending report and delegates target handling")
        fun rejectReport_success() {
            val report = report(ReportStatus.PENDING)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<Report>?>(reportRepository!!.findById(100L))
                .willReturn(Optional.of<Report>(report))

            adminReportService!!.rejectReport(1L, approveDto())

            Mockito.verify<Report?>(report).rejectReport(admin!!)
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler).handleRejected(TargetType.POST, 10L, admin!!)
        }

        @Test
        @DisplayName("throws when report is already processed")
        fun rejectReport_throwsWhenAlreadyProcessed() {
            val report = report(ReportStatus.REJECTED)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<Report>?>(reportRepository!!.findById(100L))
                .willReturn(Optional.of<Report>(report))

            assertServiceError(
                Runnable { adminReportService!!.rejectReport(1L, approveDto()) },
                ReportErrorCode.REPORT_409_ALREADY_REPORT
            )
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler, Mockito.never())
                .handleRejected(TargetType.POST, 10L, admin!!)
        }
    }

    @Nested
    @DisplayName("approveReportGroup")
    internal inner class ApproveReportGroup {
        @Test
        @DisplayName("bulk resolves pending reports and delegates target handling")
        fun approveReportGroup_success() {
            val dto = AdminReportRequestDTO(10L, TargetType.POST, "note", SanctionType.WARNED, null)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Boolean?>(reportTargetHandler!!.exists(TargetType.POST, 10L)).willReturn(true)
            BDDMockito.given<Int?>(
                reportRepository!!.updateStatusGroup(
                    TargetType.POST,
                    10L,
                    admin!!,
                    ReportStatus.RESOLVED,
                    ReportStatus.PENDING
                )
            )
                .willReturn(2)

            adminReportService!!.approveReportGroup(1L, dto)

            Mockito.verify<ReportRepository?>(reportRepository)
                .updateStatusGroup(TargetType.POST, 10L, admin!!, ReportStatus.RESOLVED, ReportStatus.PENDING)
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler)
                .handleApproved(TargetType.POST, 10L, admin!!, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when there are no pending reports to approve")
        fun approveReportGroup_throwsWhenNoPendingReports() {
            val dto = AdminReportRequestDTO(10L, TargetType.POST, "note", SanctionType.WARNED, null)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Boolean?>(reportTargetHandler!!.exists(TargetType.POST, 10L)).willReturn(true)
            BDDMockito.given<Int?>(
                reportRepository!!.updateStatusGroup(
                    TargetType.POST,
                    10L,
                    admin!!,
                    ReportStatus.RESOLVED,
                    ReportStatus.PENDING
                )
            )
                .willReturn(0)

            assertServiceError(
                Runnable { adminReportService!!.approveReportGroup(1L, dto) },
                ReportErrorCode.REPORT_404_PENDING_LIST
            )
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler, Mockito.never())
                .handleApproved(TargetType.POST, 10L, admin!!, SanctionType.WARNED, null)
        }
    }

    @Nested
    @DisplayName("rejectReportGroup")
    internal inner class RejectReportGroup {
        @Test
        @DisplayName("bulk rejects pending reports and delegates target handling")
        fun rejectReportGroup_success() {
            val dto = AdminReportRequestDTO(10L, TargetType.POST, "note", null, null)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Int?>(
                reportRepository!!.updateStatusGroup(
                    TargetType.POST,
                    10L,
                    admin!!,
                    ReportStatus.REJECTED,
                    ReportStatus.PENDING
                )
            )
                .willReturn(1)

            adminReportService!!.rejectReportGroup(1L, dto)

            Mockito.verify<ReportRepository?>(reportRepository)
                .updateStatusGroup(TargetType.POST, 10L, admin!!, ReportStatus.REJECTED, ReportStatus.PENDING)
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler).handleRejected(TargetType.POST, 10L, admin!!)
        }

        @Test
        @DisplayName("throws when there are no pending reports to reject")
        fun rejectReportGroup_throwsWhenNoPendingReports() {
            val dto = AdminReportRequestDTO(10L, TargetType.POST, "note", null, null)
            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Int?>(
                reportRepository!!.updateStatusGroup(
                    TargetType.POST,
                    10L,
                    admin!!,
                    ReportStatus.REJECTED,
                    ReportStatus.PENDING
                )
            )
                .willReturn(0)

            assertServiceError(
                Runnable { adminReportService!!.rejectReportGroup(1L, dto) },
                ReportErrorCode.REPORT_404_PENDING_LIST
            )
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler, Mockito.never())
                .handleRejected(TargetType.POST, 10L, admin!!)
        }
    }

    @Nested
    @DisplayName("approveReportGroupById")
    internal inner class ApproveReportGroupById {
        @Test
        @DisplayName("approves report group and resolves pending reports during transition")
        fun approveReportGroupById_success() {
            val reportGroup = reportGroup()
            val request =
                ApproveReportGroupRequest("note", SanctionType.WARNED, null)

            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<ReportGroup>?>(reportGroupRepository!!.findById(10L))
                .willReturn(Optional.of<ReportGroup>(reportGroup))
            BDDMockito.given<Boolean?>(reportTargetHandler!!.exists(TargetType.POST, 100L)).willReturn(true)
            BDDMockito.given<Int?>(
                reportRepository!!.updateStatusByReportGroupId(
                    10L,
                    admin!!,
                    ReportStatus.RESOLVED,
                    ReportStatus.PENDING
                )
            )
                .willReturn(2)

            adminReportService!!.approveReportGroupById(1L, 10L, request)

            Assertions.assertThat<ReportGroupStatus>(reportGroup.status).isEqualTo(ReportGroupStatus.APPROVED)
            Mockito.verify<ReportGroupRepository?>(reportGroupRepository).saveAndFlush<ReportGroup?>(reportGroup)
            Mockito.verify<ReportGroupActionRepository?>(reportGroupActionRepository)
                .save<ReportGroupAction>(ArgumentMatchers.argThat<ReportGroupAction>(ArgumentMatcher { action: ReportGroupAction ->
                    action.actionType == ReportGroupActionType.APPROVE && action.beforeStatus == ReportGroupStatus.OPEN && action.afterStatus == ReportGroupStatus.APPROVED && action.note == "note"
                            && action.sanctionType == SanctionType.WARNED && action.suspensionDays == null
                }
                ))
            Mockito.verify<ReportRepository?>(reportRepository)
                .updateStatusByReportGroupId(10L, admin!!, ReportStatus.RESOLVED, ReportStatus.PENDING)
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler)
                .handleApproved(TargetType.POST, 100L, admin!!, SanctionType.WARNED, null)
        }

        @Test
        @DisplayName("throws when report group does not exist")
        fun approveReportGroupById_throwsWhenGroupMissing() {
            val request =
                ApproveReportGroupRequest("note", SanctionType.WARNED, null)

            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<ReportGroup>?>(reportGroupRepository!!.findById(10L))
                .willReturn(Optional.empty<ReportGroup>())

            assertServiceError(
                Runnable { adminReportService!!.approveReportGroupById(1L, 10L, request) },
                ReportErrorCode.REPORT_404_REPORT_GROUP
            )
            Mockito.verify<ReportRepository?>(reportRepository, Mockito.never()).updateStatusByReportGroupId(
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.any<Member>(),
                ArgumentMatchers.any<ReportStatus>(),
                ArgumentMatchers.any<ReportStatus>()
            )
            Mockito.verifyNoInteractions(reportTargetHandler)
        }
    }

    @Nested
    @DisplayName("rejectReportGroupById")
    internal inner class RejectReportGroupById {
        @Test
        @DisplayName("rejects report group and rejects pending reports during transition")
        fun rejectReportGroupById_success() {
            val reportGroup = reportGroup()
            val request =
                RejectReportGroupRequest("not enough evidence")

            BDDMockito.given<Optional<Member>?>(memberRepository!!.findById(1L))
                .willReturn(Optional.of<Member>(admin!!))
            BDDMockito.given<Optional<ReportGroup>?>(reportGroupRepository!!.findById(10L))
                .willReturn(Optional.of<ReportGroup>(reportGroup))
            BDDMockito.given<Int?>(
                reportRepository!!.updateStatusByReportGroupId(
                    10L,
                    admin!!,
                    ReportStatus.REJECTED,
                    ReportStatus.PENDING
                )
            )
                .willReturn(2)

            adminReportService!!.rejectReportGroupById(1L, 10L, request)

            Assertions.assertThat<ReportGroupStatus>(reportGroup.status).isEqualTo(ReportGroupStatus.REJECTED)
            Mockito.verify<ReportGroupRepository?>(reportGroupRepository).saveAndFlush<ReportGroup?>(reportGroup)
            Mockito.verify<ReportGroupActionRepository?>(reportGroupActionRepository)
                .save<ReportGroupAction>(ArgumentMatchers.argThat<ReportGroupAction>(ArgumentMatcher { action: ReportGroupAction ->
                    action.actionType == ReportGroupActionType.REJECT && action.beforeStatus == ReportGroupStatus.OPEN && action.afterStatus == ReportGroupStatus.REJECTED && action.note == "not enough evidence"
                            && action.sanctionType == null && action.suspensionDays == null
                }
                ))
            Mockito.verify<ReportRepository?>(reportRepository)
                .updateStatusByReportGroupId(10L, admin!!, ReportStatus.REJECTED, ReportStatus.PENDING)
            Mockito.verify<ReportTargetHandler?>(reportTargetHandler).handleRejected(TargetType.POST, 100L, admin!!)
        }
    }

    private fun reportGroup(): ReportGroup {
        return ReportGroup(
            TargetType.POST,
            100L,
            LocalDateTime.of(2026, 1, 1, 0, 0)
        )
    }

    private fun reportGroup(
        reportGroupId: Long?,
        targetType: TargetType,
        targetId: Long,
        reportCount: Int,
        latestReportedAt: LocalDateTime
    ): ReportGroup {
        val reportGroup =
            ReportGroup(targetType, targetId, latestReportedAt)

        for (i in 0..<reportCount) {
            reportGroup.registerReport(latestReportedAt)
        }

        setField(reportGroup, "reportGroupId", reportGroupId)
        return reportGroup
    }

    private fun reasonStat(
        reportGroupId: Long,
        reasonType: String,
        reasonCount: Long
    ): ReportReasonStatProjection {
        return object : ReportReasonStatProjection {
            val reportGroupId: Long
                get() = reportGroupId

            val reasonType: String
                get() = reasonType

            val reasonCount: Long
                get() = reasonCount
        }
    }

    private fun setField(target: Any, fieldName: String, value: Any?) {
        try {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.setAccessible(true)
            field.set(target, value)
        } catch (e: ReflectiveOperationException) {
            throw IllegalStateException(e)
        }
    }

    private fun report(status: ReportStatus?): Report {
        val report = Mockito.mock<Report>(Report::class.java)
        BDDMockito.given<ReportStatus>(report.status).willReturn(status)
        BDDMockito.given<TargetType>(report.targetType).willReturn(TargetType.POST)
        BDDMockito.given<Long?>(report.targetId).willReturn(10L)
        return report
    }

    private fun approveDto(): AdminReportRequestDTO {
        return AdminReportRequestDTO(100L, TargetType.POST, "note", SanctionType.WARNED, null)
    }

    private fun assertServiceError(action: Runnable, expectedErrorCode: ErrorCodeSpec?) {
        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable { action.run() })
            .isInstanceOf(ApiException::class.java)
            .extracting<ErrorCodeSpec> { e: Throwable? -> (e as ApiException).errorCode }
            .isEqualTo(expectedErrorCode)
    }
}
