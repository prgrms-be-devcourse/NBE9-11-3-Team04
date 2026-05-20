package com.back.devc.domain.interaction.report.repository

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.member.member.entity.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ReportRepository : JpaRepository<Report, Long> {

    fun findAllByStatus(
        status: ReportStatus,
        pageable: Pageable,
    ): Page<Report>

    @Deprecated(
        message = "Use ReportGroupRepository.findReportGroups instead."
    )
    @Query(
        value = """
        SELECT r.targetType as targetType,
               r.targetId as targetId,
               COUNT(r) as reportCount,
               MAX(r.createdAt) as latestCreatedAt
        FROM Report r
        WHERE (:status IS NULL OR r.status = :status)
          AND r.createdAt >= :from
          AND r.createdAt < :to
        GROUP BY r.targetType, r.targetId
        ORDER BY latestCreatedAt DESC, r.targetType ASC, r.targetId DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT CONCAT(r.targetType, '-', r.targetId))
        FROM Report r
        WHERE (:status IS NULL OR r.status = :status)
          AND r.createdAt >= :from
          AND r.createdAt < :to
        """
    )
    fun findGroupedReports(
        @Param("status") status: ReportStatus?,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
        pageable: Pageable,
    ): Page<Array<Any>>

    fun findAllByTargetTypeAndTargetIdAndStatus(
        targetType: TargetType,
        targetId: Long,
        reportStatus: ReportStatus,
    ): List<Report>

    fun findAllByTargetTypeAndTargetId(
        targetType: TargetType,
        targetId: Long,
    ): List<Report>

    fun existsByReporterAndTargetTypeAndTargetId(
        reporter: Member,
        targetType: TargetType,
        targetId: Long,
    ): Boolean

    @Deprecated(
        message = "Use findReasonStatsByReportGroupIds instead."
    )
    @Query(
        """
        SELECT r.targetType, r.targetId, r.reasonType
        FROM Report r
        WHERE (r.targetType = :postType AND r.targetId IN :postIds)
           OR (r.targetType = :commentType AND r.targetId IN :commentIds)
        """
    )
    fun findReasonTypesBatch(
        @Param("postType") postType: TargetType,
        @Param("postIds") postIds: List<Long>,
        @Param("commentType") commentType: TargetType,
        @Param("commentIds") commentIds: List<Long>,
    ): List<Array<Any>>

    // N+1 처리 전 사용한 조회 방법
    @Deprecated(
        message = "Use findReasonStatsByReportGroupIds instead."
    )
    fun findReasonTypesByTargetId(
        targetType: TargetType,
        targetId: Long,
    ): List<String>

    @Deprecated(
        message = "Use updateStatusByReportGroupId instead."
    )
    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE Report r
        SET r.status = :newStatus,
            r.processedByAdmin = :admin,
            r.processedAt = CURRENT_TIMESTAMP
        WHERE r.targetType = :targetType
          AND r.targetId = :targetId
          AND r.status = :oldStatus
        """
    )
    fun updateStatusGroup(
        @Param("targetType") targetType: TargetType,
        @Param("targetId") targetId: Long,
        @Param("admin") admin: Member,
        @Param("newStatus") newStatus: ReportStatus,
        @Param("oldStatus") oldStatus: ReportStatus,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE Report r
        SET r.status = :newStatus,
            r.processedByAdmin = :admin,
            r.processedAt = CURRENT_TIMESTAMP
        WHERE r.reportGroup.reportGroupId = :reportGroupId
          AND r.status = :oldStatus
        """
    )
    fun updateStatusByReportGroupId(
        @Param("reportGroupId") reportGroupId: Long,
        @Param("admin") admin: Member,
        @Param("newStatus") newStatus: ReportStatus,
        @Param("oldStatus") oldStatus: ReportStatus
    ): Int

    fun existsByReporterAndReportGroup(
        reporter: Member,
        reportGroup: ReportGroup
    ): Boolean

    fun findAllByReportGroup(
        reportGroup: ReportGroup
    ): List<Report>

    @Query(
        """
    SELECT r.reportGroup.reportGroupId as reportGroupId,
           r.reasonType as reasonType,
           COUNT(r) as reasonCount
    FROM Report r
    WHERE r.reportGroup.reportGroupId IN :reportGroupIds
    GROUP BY r.reportGroup.reportGroupId, r.reasonType
    ORDER BY COUNT(r) DESC
    """
    )
    fun findReasonStatsByReportGroupIds(
        @Param("reportGroupIds") reportGroupIds: List<Long>
    ): List<ReportReasonStatProjection>
}
