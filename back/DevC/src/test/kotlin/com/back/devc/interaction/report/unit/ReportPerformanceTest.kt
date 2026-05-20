package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.ReportGroupResponseDTO
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportReasonStatProjection
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.interaction.report.service.AdminReportService
import com.back.devc.domain.interaction.report.util.ReportTargetHandler
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.interaction.report.setPrivateField
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.StopWatch
import java.time.LocalDateTime


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Report group lookup performance smoke test")
internal class ReportPerformanceTest {
    private val reportRepository = mock<ReportRepository>()
    private val reportGroupRepository = mock<ReportGroupRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val postRepository = mock<PostRepository>()
    private val commentRepository = mock<CommentRepository>()
    private val reportTargetHandler = mock<ReportTargetHandler>()

    private val adminReportService = AdminReportService(
        reportRepository = reportRepository,
        reportGroupRepository = reportGroupRepository,
        reportGroupActionRepository = mock(),
        memberRepository = memberRepository,
        postRepository = postRepository,
        commentRepository = commentRepository,
        reportTargetHandler = reportTargetHandler
    )

    @Test
    @DisplayName("ReportGroup 기반 목록 조회는 현재 페이지 대상만 batch 로딩한다")
    fun getGroupedReportsPerformanceTest() {
        val status = ReportStatus.PENDING
        val pageNumber = 400
        val pageSize = 20
        val totalData = 10_000L
        val pageable = PageRequest.of(pageNumber, pageSize)
        val pageIds = pageTargetIds(pageNumber, pageSize)
        val reportGroups = pageIds.mapIndexed { index, targetId ->
            reportGroup(index + 1L, targetId)
        }
        val posts = pageIds.map(::post)

        whenever(reportGroupRepository.findReportGroups(eq(ReportGroupStatus.OPEN), any(), any(), any()))
            .thenReturn(PageImpl(reportGroups, pageable, totalData))
        whenever(postRepository.findAllByPostIdIn(pageIds)).thenReturn(posts)
        whenever(reportRepository.findReasonStatsByReportGroupIds(reportGroups.map { it.reportGroupIdRequired() }))
            .thenReturn(reportGroups.map { reasonStat(it.reportGroupIdRequired(), "SPAM", 1L) })

        val stopWatch = StopWatch()
        stopWatch.start()

        val result: Page<ReportGroupResponseDTO> = adminReportService.getGroupedReports(status, pageable)

        stopWatch.stop()

        println("ReportGroup batch lookup: total=${result.totalElements}, elapsed=${stopWatch.totalTimeMillis}ms")
        assertThat(result.content).hasSize(pageSize)
        assertThat(result.totalElements).isEqualTo(totalData)
    }

    @Test
    @DisplayName("legacy groupBy 목록 조회는 target handler를 행마다 호출한다")
    fun getGroupedReportsNoBatchPerformanceTest() {
        val status = ReportStatus.PENDING
        val pageNumber = 400
        val pageSize = 20
        val totalData = 10_000L
        val pageable = PageRequest.of(pageNumber, pageSize)
        val rows = pageTargetIds(pageNumber, pageSize).map { targetId ->
            arrayOf<Any>(TargetType.POST, targetId, 5L, LocalDateTime.now())
        }
        val mockInfo = ReportTargetHandler.TargetInfo("writer", "title", "content")

        whenever(reportRepository.findGroupedReports(eq(status), any(), any(), any<Pageable>()))
            .thenReturn(PageImpl(rows, pageable, totalData))
        rows.forEach { row ->
            val targetId = row[1] as Long
            whenever(reportTargetHandler.getTargetInfo(TargetType.POST, targetId)).thenReturn(mockInfo)
            whenever(reportRepository.findReasonTypesByTargetId(TargetType.POST, targetId))
                .thenReturn(listOf("SPAM", "ABUSE"))
        }

        val stopWatch = StopWatch()
        stopWatch.start()

        val result: Page<ReportGroupResponseDTO> = adminReportService.getGroupedReportsNoBatch(status, pageable)

        stopWatch.stop()

        println("Legacy groupBy lookup: total=${result.totalElements}, elapsed=${stopWatch.totalTimeMillis}ms")
        assertThat(result.content).hasSize(pageSize)
        assertThat(result.totalElements).isEqualTo(totalData)

        rows.forEach { row ->
            val targetId = row[1] as Long
            verify(reportTargetHandler).getTargetInfo(TargetType.POST, targetId)
            verify(reportRepository).findReasonTypesByTargetId(TargetType.POST, targetId)
        }
    }

    private fun pageTargetIds(pageNumber: Int, pageSize: Int): List<Long> {
        val start = pageNumber * pageSize + 1L
        return (start until start + pageSize).toList()
    }

    private fun post(postId: Long): Post {
        val writer = mock<Member>()
        val post = mock<Post>()
        whenever(writer.nickname).thenReturn("writer-$postId")
        whenever(post.postId).thenReturn(postId)
        whenever(post.title).thenReturn("title-$postId")
        whenever(post.content).thenReturn("content-$postId")
        whenever(post.member).thenReturn(writer)
        return post
    }

    private fun reportGroup(reportGroupId: Long, targetId: Long): ReportGroup {
        val reportGroup = ReportGroup(TargetType.POST, targetId, LocalDateTime.now())
        reportGroup.registerReport(LocalDateTime.now())
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

    private fun ReportGroup.reportGroupIdRequired(): Long =
        reportGroupId ?: throw AssertionError("Expected reportGroupId")

}
