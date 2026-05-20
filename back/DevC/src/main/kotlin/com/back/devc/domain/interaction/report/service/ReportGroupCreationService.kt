package com.back.devc.domain.interaction.report.service

import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ReportGroupCreationService(
    private val reportGroupRepository: ReportGroupRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createOpenReportGroup(
        targetType: TargetType,
        targetId: Long,
        firstReportedAt: LocalDateTime
    ): ReportGroup =
        reportGroupRepository.saveAndFlush(
            ReportGroup(
                targetType = targetType,
                targetId = targetId,
                firstReportedAt = firstReportedAt
            )
        )
}
