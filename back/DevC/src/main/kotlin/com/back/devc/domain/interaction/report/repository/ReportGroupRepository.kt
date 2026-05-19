package com.back.devc.domain.interaction.report.repository

import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ReportGroupRepository : JpaRepository<ReportGroup, Long> {
    fun findByTargetTypeAndTargetId(
        targetType: TargetType,
        targetId: Long
    ): ReportGroup?

    fun findAllByStatus(
        status: ReportGroupStatus,
        pageable: Pageable
    ): Page<ReportGroup>

    fun existsByTargetTypeAndTargetId(
        targetType: TargetType,
        targetId: Long
    ): Boolean
}