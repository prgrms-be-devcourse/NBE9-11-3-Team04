package com.back.devc.domain.interaction.report.integration;

import com.back.devc.domain.interaction.report.entity.ReportGroup;
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ReportGroupOptimisticLockTest {

    @Autowired
    private ReportGroupRepository reportGroupRepository;

    @Autowired
    private MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("same ReportGroup processed from two stale views allows only the first update")
    void optimisticLock_whenSameReportGroupIsProcessedFromTwoStaleViews() {
        Member admin = saveAdmin();
        ReportGroup savedReportGroup = saveReportGroup();
        Long reportGroupId = savedReportGroup.getReportGroupId();

        entityManager.clear();

        ReportGroup firstView = reportGroupRepository.findById(reportGroupId).orElseThrow();
        entityManager.detach(firstView);

        ReportGroup secondView = reportGroupRepository.findById(reportGroupId).orElseThrow();
        entityManager.detach(secondView);

        firstView.approve(
                admin,
                "approve first",
                null,
                null,
                LocalDateTime.of(2026, 1, 1, 1, 0)
        );

        reportGroupRepository.saveAndFlush(firstView);
        entityManager.clear();

        secondView.reject(
                admin,
                "reject second",
                LocalDateTime.of(2026, 1, 1, 1, 1)
        );

        assertThatThrownBy(() -> reportGroupRepository.saveAndFlush(secondView))
                .isInstanceOfAny(
                        ObjectOptimisticLockingFailureException.class,
                        OptimisticLockException.class
                );

        entityManager.clear();

        ReportGroup result = reportGroupRepository.findById(reportGroupId).orElseThrow();

        assertThat(result.getStatus()).isEqualTo(ReportGroupStatus.APPROVED);
        assertThat(result.getVersion()).isEqualTo(1L);
    }

    private Member saveAdmin() {
        Member admin = Member.createLocalAdminMember(
                "admin@test.com",
                "password",
                "admin"
        );

        return memberRepository.saveAndFlush(admin);
    }

    private ReportGroup saveReportGroup() {
        LocalDateTime firstReportedAt =
                LocalDateTime.of(2026, 1, 1, 0, 0);
        ReportGroup reportGroup = new ReportGroup(
                TargetType.POST,
                100L,
                firstReportedAt
        );
        reportGroup.registerReport(firstReportedAt);

        return reportGroupRepository.saveAndFlush(reportGroup);
    }
}
