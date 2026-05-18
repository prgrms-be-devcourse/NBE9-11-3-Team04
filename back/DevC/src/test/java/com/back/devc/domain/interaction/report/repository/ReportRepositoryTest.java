package com.back.devc.domain.interaction.report.repository;

import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void findGroupedReports_filtersByStatusAndCreatedAtRange() {
        Member reporter1 = memberRepository.save(Member.createLocalMember(
                "reporter1@test.com",
                "password123!",
                "reporter1"
        ));
        Member reporter2 = memberRepository.save(Member.createLocalMember(
                "reporter2@test.com",
                "password123!",
                "reporter2"
        ));
        Member reporter3 = memberRepository.save(Member.createLocalMember(
                "reporter3@test.com",
                "password123!",
                "reporter3"
        ));

        Report rejectedReport = report(reporter3, TargetType.POST, 2L);
        rejectedReport.rejectReport(reporter3);

        reportRepository.save(report(reporter1, TargetType.POST, 1L));
        reportRepository.save(report(reporter2, TargetType.POST, 1L));
        reportRepository.save(rejectedReport);

        LocalDateTime now = LocalDateTime.now();

        Page<Object[]> page = reportRepository.findGroupedReports(
                ReportStatus.PENDING,
                now.minusDays(1),
                now.plusDays(1),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        Object[] row = page.getContent().getFirst();
        assertThat(row[0]).isEqualTo(TargetType.POST);
        assertThat(row[1]).isEqualTo(1L);
        assertThat(row[2]).isEqualTo(2L);

        Page<Object[]> futurePage = reportRepository.findGroupedReports(
                ReportStatus.PENDING,
                now.plusDays(1),
                now.plusDays(2),
                PageRequest.of(0, 10)
        );

        assertThat(futurePage.getTotalElements()).isZero();
    }

    private Report report(Member reporter, TargetType targetType, Long targetId) {
        return Report.create(
                reporter,
                TargetType.POST,
                10L,
                "SPAM",
                "Repeated promotion"
        );
    }
}
