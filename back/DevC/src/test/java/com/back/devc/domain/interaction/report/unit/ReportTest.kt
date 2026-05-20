package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.Report.Companion.create
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalAdminMember
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalMember
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Report")
internal class ReportTest {
    @Test
    @DisplayName("create creates a pending report")
    fun create_createsPendingReport() {
        val reporter = createLocalMember(
            "reporter@test.com",
            "password",
            "reporter"
        )

        val report = create(
            reporter,
            TargetType.POST,
            10L,
            "SPAM",
            "Repeated promotion"
        )

        Assertions.assertThat<Member>(report.reporter).isSameAs(reporter)
        Assertions.assertThat<TargetType>(report.targetType).isEqualTo(TargetType.POST)
        Assertions.assertThat(report.targetId).isEqualTo(10L)
        Assertions.assertThat(report.reasonType).isEqualTo("SPAM")
        Assertions.assertThat(report.reasonDetail).isEqualTo("Repeated promotion")

        Assertions.assertThat<ReportStatus>(report.status).isEqualTo(ReportStatus.PENDING)
        Assertions.assertThat<Member?>(report.processedByAdmin).isNull()
        Assertions.assertThat(report.processedAt).isNull()
    }

    @Test
    @DisplayName("processReport resolves the report and records admin")
    fun processReport_resolvesReport() {
        val report = pendingReport()

        val admin = createLocalAdminMember(
            "admin@test.com",
            "password",
            "admin"
        )

        report.processReport(admin)

        Assertions.assertThat<ReportStatus>(report.status).isEqualTo(ReportStatus.RESOLVED)
        Assertions.assertThat<Member?>(report.processedByAdmin).isSameAs(admin)
        Assertions.assertThat(report.processedAt).isNotNull()
    }

    @Test
    @DisplayName("rejectReport rejects the report and records admin")
    fun rejectReport_rejectsReport() {
        val report = pendingReport()

        val admin = createLocalAdminMember(
            "admin@test.com",
            "password",
            "admin"
        )

        report.rejectReport(admin)

        Assertions.assertThat<ReportStatus>(report.status).isEqualTo(ReportStatus.REJECTED)
        Assertions.assertThat<Member?>(report.processedByAdmin).isSameAs(admin)
        Assertions.assertThat(report.processedAt).isNotNull()
    }

    private fun pendingReport(): Report {
        return create(
            createLocalMember(
                "reporter@test.com",
                "password",
                "reporter"
            ),
            TargetType.COMMENT,
            20L,
            "ABUSE",
            "Insulting content"
        )
    }
}