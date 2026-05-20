package com.back.devc.domain.interaction.report.integration

import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalAdminMember
import com.back.devc.domain.member.member.repository.MemberRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.OptimisticLockException
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
internal class ReportGroupOptimisticLockTest {
    @Autowired
    private val reportGroupRepository: ReportGroupRepository? = null

    @Autowired
    private val memberRepository: MemberRepository? = null

    @PersistenceContext
    private val entityManager: EntityManager? = null

    @Test
    @DisplayName("same ReportGroup processed from two stale views allows only the first update")
    fun optimisticLock_whenSameReportGroupIsProcessedFromTwoStaleViews() {
        val admin = saveAdmin()
        val savedReportGroup = saveReportGroup()
        val reportGroupId = savedReportGroup.reportGroupId

        entityManager!!.clear()

        val firstView = reportGroupRepository!!.findById(reportGroupId).orElseThrow()
        entityManager.detach(firstView)

        val secondView = reportGroupRepository.findById(reportGroupId).orElseThrow()
        entityManager.detach(secondView)

        firstView.approve(
            admin,
            "approve first",
            null,
            null,
            LocalDateTime.of(2026, 1, 1, 1, 0)
        )

        reportGroupRepository.saveAndFlush<ReportGroup?>(firstView)
        entityManager.clear()

        secondView.reject(
            admin,
            "reject second",
            LocalDateTime.of(2026, 1, 1, 1, 1)
        )

        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable {
            reportGroupRepository.saveAndFlush<ReportGroup?>(
                secondView
            )
        })
            .isInstanceOfAny(
                ObjectOptimisticLockingFailureException::class.java,
                OptimisticLockException::class.java
            )

        entityManager.clear()

        val result = reportGroupRepository.findById(reportGroupId).orElseThrow()

        Assertions.assertThat<ReportGroupStatus>(result.status).isEqualTo(ReportGroupStatus.APPROVED)
        Assertions.assertThat(result.version).isEqualTo(1L)
    }

    private fun saveAdmin(): Member {
        val admin = createLocalAdminMember(
            "admin@test.com",
            "password",
            "admin"
        )

        return memberRepository!!.saveAndFlush<Member>(admin)
    }

    private fun saveReportGroup(): ReportGroup {
        val firstReportedAt =
            LocalDateTime.of(2026, 1, 1, 0, 0)
        val reportGroup = ReportGroup(
            TargetType.POST,
            100L,
            firstReportedAt
        )
        reportGroup.registerReport(firstReportedAt)

        return reportGroupRepository!!.saveAndFlush<ReportGroup>(reportGroup)
    }
}
