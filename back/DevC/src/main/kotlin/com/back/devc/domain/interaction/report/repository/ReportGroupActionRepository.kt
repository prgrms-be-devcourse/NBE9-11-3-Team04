package com.back.devc.domain.interaction.report.repository

import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupAction
import org.springframework.data.jpa.repository.JpaRepository

interface ReportGroupActionRepository : JpaRepository<ReportGroupAction, Long> {
    fun findAllByReportGroupOrderByCreatedAtAsc(
        reportGroup: ReportGroup
    ): List<ReportGroupAction>
}
