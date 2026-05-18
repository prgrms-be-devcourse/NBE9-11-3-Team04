package com.back.devc.domain.interaction.report.entity;

import com.back.devc.domain.member.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Report")
class ReportTest {

    @Test
    @DisplayName("create creates a pending report")
    void create_createsPendingReport() {
        Member reporter = Member.createLocalMember(
                "reporter@test.com",
                "password",
                "reporter"
        );

        Report report = Report.create(
                reporter,
                TargetType.POST,
                10L,
                "SPAM",
                "Repeated promotion"
        );

        assertThat(report.getReporter()).isSameAs(reporter);
        assertThat(report.getTargetType()).isEqualTo(TargetType.POST);
        assertThat(report.getTargetId()).isEqualTo(10L);
        assertThat(report.getReasonType()).isEqualTo("SPAM");
        assertThat(report.getReasonDetail()).isEqualTo("Repeated promotion");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(report.getProcessedByAdmin()).isNull();
        assertThat(report.getProcessedAt()).isNull();
    }

    @Test
    @DisplayName("processReport resolves the report and records admin")
    void processReport_resolvesReport() {
        Report report = pendingReport();

        Member admin = Member.createLocalAdminMember(
                "admin@test.com",
                "password",
                "admin"
        );

        report.processReport(admin);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getProcessedByAdmin()).isSameAs(admin);
        assertThat(report.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("rejectReport rejects the report and records admin")
    void rejectReport_rejectsReport() {
        Report report = pendingReport();

        Member admin = Member.createLocalAdminMember(
                "admin@test.com",
                "password",
                "admin"
        );

        report.rejectReport(admin);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
        assertThat(report.getProcessedByAdmin()).isSameAs(admin);
        assertThat(report.getProcessedAt()).isNotNull();
    }

    private Report pendingReport() {
        return Report.create(
                Member.createLocalMember(
                        "reporter@test.com",
                        "password",
                        "reporter"
                ),
                TargetType.COMMENT,
                20L,
                "ABUSE",
                "Insulting content"
        );
    }
}