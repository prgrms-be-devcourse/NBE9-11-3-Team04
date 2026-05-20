package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.member.member.entity.Member
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest
@DisplayName("Report")
internal class ReportTest {

    @Test
    @DisplayName("create creates a pending report")
    fun create_createsPendingReport() {
        val reporter = member()

        val report = Report.create(
            reporter = reporter,
            targetType = TargetType.POST,
            targetId = 10L,
            reasonType = "SPAM",
            reasonDetail = "Repeated promotion"
        )

        assertThat(report.reporter).isSameAs(reporter)
        assertThat(report.targetType).isEqualTo(TargetType.POST)
        assertThat(report.targetId).isEqualTo(10L)
        assertThat(report.reasonType).isEqualTo("SPAM")
        assertThat(report.reasonDetail).isEqualTo("Repeated promotion")
        assertThat(report.status).isEqualTo(ReportStatus.PENDING)
        assertThat(report.processedByAdmin).isNull()
        assertThat(report.processedAt).isNull()
    }

    @Test
    @DisplayName("processReport resolves the report and records admin")
    fun processReport_resolvesReport() {
        val report = pendingReport()
        val admin = admin()

        report.processReport(admin)

        assertThat(report.status).isEqualTo(ReportStatus.RESOLVED)
        assertThat(report.processedByAdmin).isSameAs(admin)
        assertThat(report.processedAt).isNotNull()
    }

    @Test
    @DisplayName("rejectReport rejects the report and records admin")
    fun rejectReport_rejectsReport() {
        val report = pendingReport()
        val admin = admin()

        report.rejectReport(admin)

        assertThat(report.status).isEqualTo(ReportStatus.REJECTED)
        assertThat(report.processedByAdmin).isSameAs(admin)
        assertThat(report.processedAt).isNotNull()
    }

    private fun pendingReport(): Report =
        Report.create(
            reporter = member(),
            targetType = TargetType.COMMENT,
            targetId = 20L,
            reasonType = "ABUSE",
            reasonDetail = "Insulting content"
        )

    private fun member(): Member =
        Member.createLocalMember(
            email = "reporter@test.com",
            passwordHash = "password",
            nickname = "reporter"
        )

    private fun admin(): Member =
        Member.createLocalAdminMember(
            email = "admin@test.com",
            passwordHash = "password",
            nickname = "admin"
        )
}
