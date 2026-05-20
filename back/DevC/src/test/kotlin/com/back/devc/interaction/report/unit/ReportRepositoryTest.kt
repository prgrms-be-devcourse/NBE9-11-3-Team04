package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
internal class ReportRepositoryTest @Autowired constructor(
    private val reportRepository: ReportRepository,
    private val reportGroupRepository: ReportGroupRepository,
    private val memberRepository: MemberRepository
) {

    @Test
    fun findGroupedReports_filtersByStatusAndCreatedAtRange() {
        val reporter1 = saveMember("reporter1@test.com", "reporter1")
        val reporter2 = saveMember("reporter2@test.com", "reporter2")
        val reporter3 = saveMember("reporter3@test.com", "reporter3")

        val rejectedReport = report(reporter3, TargetType.POST, 2L)
        rejectedReport.rejectReport(reporter3)

        reportRepository.save(report(reporter1, TargetType.POST, 1L))
        reportRepository.save(report(reporter2, TargetType.POST, 1L))
        reportRepository.save(rejectedReport)

        val now = LocalDateTime.now()

        val page = reportRepository.findGroupedReports(
            status = ReportStatus.PENDING,
            from = now.minusDays(1),
            to = now.plusDays(1),
            pageable = PageRequest.of(0, 10)
        )

        assertThat(page.totalElements).isEqualTo(1)
        assertThat(page.content).hasSize(1)

        val row = page.content.first()
        assertThat(row[0]).isEqualTo(TargetType.POST)
        assertThat(row[1]).isEqualTo(1L)
        assertThat(row[2]).isEqualTo(2L)

        val futurePage = reportRepository.findGroupedReports(
            status = ReportStatus.PENDING,
            from = now.plusDays(1),
            to = now.plusDays(2),
            pageable = PageRequest.of(0, 10)
        )

        assertThat(futurePage.totalElements).isZero()
    }

    private fun saveMember(email: String, nickname: String): Member =
        memberRepository.save(
            Member.createLocalMember(
                email = email,
                passwordHash = "password123!",
                nickname = nickname
            )
        )

    private fun report(
        reporter: Member,
        targetType: TargetType,
        targetId: Long
    ): Report {
        val report = Report.create(
            reporter = reporter,
            targetType = targetType,
            targetId = targetId,
            reasonType = "SPAM",
            reasonDetail = "Repeated promotion"
        )
        report.assignReportGroup(reportGroup(targetType, targetId))
        return report
    }

    private fun reportGroup(targetType: TargetType, targetId: Long): ReportGroup {
        val reportedAt = LocalDateTime.now()
        val reportGroup = reportGroupRepository.findByTargetTypeAndTargetId(targetType, targetId)
            ?: ReportGroup(targetType, targetId, reportedAt)

        reportGroup.registerReport(reportedAt)
        return reportGroupRepository.saveAndFlush(reportGroup)
    }
}
