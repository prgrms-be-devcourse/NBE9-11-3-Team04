package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.Report.Companion.create
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalMember
import com.back.devc.domain.member.member.repository.MemberRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
internal class ReportRepositoryTest {
    @Autowired
    private val reportRepository: ReportRepository? = null

    @Autowired
    private val reportGroupRepository: ReportGroupRepository? = null

    @Autowired
    private val memberRepository: MemberRepository? = null

    @Test
    fun findGroupedReports_filtersByStatusAndCreatedAtRange() {
        val reporter1 = memberRepository!!.save<Member>(
            createLocalMember(
                "reporter1@test.com",
                "password123!",
                "reporter1"
            )
        )
        val reporter2 = memberRepository.save<Member>(
            createLocalMember(
                "reporter2@test.com",
                "password123!",
                "reporter2"
            )
        )
        val reporter3 = memberRepository.save<Member>(
            createLocalMember(
                "reporter3@test.com",
                "password123!",
                "reporter3"
            )
        )

        val rejectedReport = report(reporter3, TargetType.POST, 2L)
        rejectedReport.rejectReport(reporter3)

        reportRepository!!.save<Report?>(report(reporter1, TargetType.POST, 1L))
        reportRepository.save<Report?>(report(reporter2, TargetType.POST, 1L))
        reportRepository.save<Report?>(rejectedReport)

        val now = LocalDateTime.now()

        val page: Page<Array<Any?>?> = reportRepository.findGroupedReports(
            ReportStatus.PENDING,
            now.minusDays(1),
            now.plusDays(1),
            PageRequest.of(0, 10)
        )

        Assertions.assertThat(page.getTotalElements()).isEqualTo(1)
        Assertions.assertThat<Array<Any?>?>(page.getContent()).hasSize(1)
        val row: Array<Any?> = page.getContent().getFirst()
        Assertions.assertThat<Any?>(row[0]).isEqualTo(TargetType.POST)
        Assertions.assertThat<Any?>(row[1]).isEqualTo(1L)
        Assertions.assertThat<Any?>(row[2]).isEqualTo(2L)

        val futurePage: Page<Array<Any?>?> = reportRepository.findGroupedReports(
            ReportStatus.PENDING,
            now.plusDays(1),
            now.plusDays(2),
            PageRequest.of(0, 10)
        )

        Assertions.assertThat(futurePage.getTotalElements()).isZero()
    }

    private fun report(reporter: Member, targetType: TargetType, targetId: Long): Report {
        val report = create(
            reporter,
            targetType,
            targetId,
            "SPAM",
            "Repeated promotion"
        )
        report.assignReportGroup(reportGroup(targetType, targetId))
        return report
    }

    private fun reportGroup(targetType: TargetType, targetId: Long): ReportGroup {
        val reportedAt = LocalDateTime.now()

        var reportGroup = reportGroupRepository!!
            .findByTargetTypeAndTargetId(targetType, targetId)

        if (reportGroup == null) {
            reportGroup = ReportGroup(targetType, targetId, reportedAt)
        }

        reportGroup.registerReport(reportedAt)
        return reportGroupRepository.saveAndFlush<ReportGroup>(reportGroup)
    }
}
