package com.back.devc.interaction.report.integration

import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.interaction.report.orThrow
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.OptimisticLockException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
internal class ReportGroupOptimisticLockTest @Autowired constructor(
    private val reportGroupRepository: ReportGroupRepository,
    private val memberRepository: MemberRepository,
    private val entityManager: EntityManager
) {

    @Test
    @DisplayName("same ReportGroup processed from two stale views allows only the first update")
    fun optimisticLock_whenSameReportGroupIsProcessedFromTwoStaleViews() {
        val admin = saveAdmin()
        val reportGroupId = saveReportGroup().reportGroupId.orThrow()

        entityManager.clear()

        val firstView = reportGroupRepository.findById(reportGroupId).orThrow()
        entityManager.detach(firstView)

        val secondView = reportGroupRepository.findById(reportGroupId).orThrow()
        entityManager.detach(secondView)

        firstView.approve(
            admin = admin,
            note = "approve first",
            sanctionType = null,
            suspensionDays = null,
            now = LocalDateTime.of(2026, 1, 1, 1, 0)
        )

        reportGroupRepository.saveAndFlush(firstView)
        entityManager.clear()

        secondView.reject(
            admin = admin,
            note = "reject second",
            now = LocalDateTime.of(2026, 1, 1, 1, 1)
        )

        assertThatThrownBy { reportGroupRepository.saveAndFlush(secondView) }
            .isInstanceOfAny(
                ObjectOptimisticLockingFailureException::class.java,
                OptimisticLockException::class.java
            )

        entityManager.clear()

        val result = reportGroupRepository.findById(reportGroupId).orThrow()

        assertThat(result.status).isEqualTo(ReportGroupStatus.APPROVED)
        assertThat(result.version).isEqualTo(1L)
    }

    private fun saveAdmin(): Member =
        memberRepository.saveAndFlush(
            Member.createLocalAdminMember(
                email = "admin@test.com",
                passwordHash = "password",
                nickname = "admin"
            )
        )

    private fun saveReportGroup(): ReportGroup {
        val firstReportedAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        val reportGroup = ReportGroup(
            targetType = TargetType.POST,
            targetId = 100L,
            firstReportedAt = firstReportedAt
        )
        reportGroup.registerReport(firstReportedAt)

        return reportGroupRepository.saveAndFlush(reportGroup)
    }
}
