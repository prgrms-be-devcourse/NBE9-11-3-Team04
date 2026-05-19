package com.back.devc.domain.interaction.report.repository

import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportGroupStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

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

    @Query(
        """
        SELECT rg
        FROM ReportGroup rg
        WHERE (:status IS NULL OR rg.status = :status)
          AND rg.latestReportedAt >= :from
          AND rg.latestReportedAt < :to
        """
    )
    fun findReportGroups(
        @Param("status") status: ReportGroupStatus?,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
        pageable: Pageable
    ): Page<ReportGroup>
}