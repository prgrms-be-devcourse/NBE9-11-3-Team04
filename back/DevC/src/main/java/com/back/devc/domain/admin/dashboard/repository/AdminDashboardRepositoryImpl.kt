package com.back.devc.domain.admin.dashboard.repository

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportReasonCount
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class AdminDashboardRepositoryImpl(
    private val em: EntityManager,
) : AdminDashboardRepository {

    private fun startOfDay(date: LocalDate): LocalDateTime = date.atStartOfDay()

    private fun endOfDay(date: LocalDate): LocalDateTime = date.plusDays(1).atStartOfDay()

    override fun countAllUsers(): Long {
        return em.createQuery("select count(m) from Member m", Long::class.javaObjectType)
            .singleResult
    }

    override fun countAllPosts(): Long {
        return em.createQuery("select count(p) from Post p", Long::class.javaObjectType)
            .singleResult
    }

    override fun countPendingReports(): Long {
        return em.createQuery(
            """
            select count(r)
            from Report r
            where r.status = :status
            """.trimIndent(),
            Long::class.javaObjectType,
        )
            .setParameter("status", ReportStatus.PENDING)
            .singleResult
    }

    override fun countTodayUsers(today: LocalDate): Long {
        return em.createQuery(
            """
            select count(m)
            from Member m
            where m.createdAt >= :start and m.createdAt < :end
            """.trimIndent(),
            Long::class.javaObjectType,
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .singleResult
    }

    override fun countTodayPosts(today: LocalDate): Long {
        return em.createQuery(
            """
            select count(p)
            from Post p
            where p.createdAt >= :start and p.createdAt < :end
            """.trimIndent(),
            Long::class.javaObjectType,
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .singleResult
    }

    override fun countTodayComments(today: LocalDate): Long {
        return em.createQuery(
            """
            select count(c)
            from Comment c
            where c.createdAt >= :start and c.createdAt < :end
            """.trimIndent(),
            Long::class.javaObjectType,
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .singleResult
    }

    override fun countTodayPostReports(today: LocalDate): Long {
        return countTodayReports(today, TargetType.POST)
    }

    override fun countTodayPostReportsByStatus(today: LocalDate, status: ReportStatus): Long {
        return countTodayReportsByStatus(today, TargetType.POST, status)
    }

    override fun countTodayPostReportsByReason(today: LocalDate): List<ReportReasonCount> {
        return countTodayReportsByReason(today, TargetType.POST)
    }

    override fun countTodayCommentReports(today: LocalDate): Long {
        return countTodayReports(today, TargetType.COMMENT)
    }

    override fun countTodayCommentReportsByStatus(today: LocalDate, status: ReportStatus): Long {
        return countTodayReportsByStatus(today, TargetType.COMMENT, status)
    }

    override fun countTodayCommentReportsByReason(today: LocalDate): List<ReportReasonCount> {
        return countTodayReportsByReason(today, TargetType.COMMENT)
    }

    private fun countTodayReports(today: LocalDate, targetType: TargetType): Long {
        return em.createQuery(
            """
            select count(r)
            from Report r
            where r.targetType = :targetType
            and r.createdAt >= :start and r.createdAt < :end
            """.trimIndent(),
            Long::class.javaObjectType,
        )
            .setParameter("targetType", targetType)
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .singleResult
    }

    private fun countTodayReportsByStatus(
        today: LocalDate,
        targetType: TargetType,
        status: ReportStatus,
    ): Long {
        return em.createQuery(
            """
            select count(r)
            from Report r
            where r.targetType = :targetType
            and r.status = :status
            and r.createdAt >= :start and r.createdAt < :end
            """.trimIndent(),
            Long::class.javaObjectType,
        )
            .setParameter("targetType", targetType)
            .setParameter("status", status)
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .singleResult
    }

    private fun countTodayReportsByReason(
        today: LocalDate,
        targetType: TargetType,
    ): List<ReportReasonCount> {
        return em.createQuery(
            """
            select new com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto${'$'}ReportReasonCount(
                r.reasonType, count(r)
            )
            from Report r
            where r.targetType = :targetType
            and r.createdAt >= :start and r.createdAt < :end
            group by r.reasonType
            order by count(r) desc
            """.trimIndent(),
            ReportReasonCount::class.java,
        )
            .setParameter("targetType", targetType)
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .resultList
    }
}
