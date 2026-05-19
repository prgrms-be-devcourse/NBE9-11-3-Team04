package com.back.devc.domain.admin.dashboard.repository

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportReasonCount
import com.back.devc.domain.interaction.report.entity.ReportStatus
import jakarta.persistence.EntityManager
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
@RequiredArgsConstructor
class AdminDashboardRepositoryImpl : AdminDashboardRepository {
    private val em: EntityManager? = null

    private fun startOfDay(date: LocalDate): LocalDateTime {
        return date.atStartOfDay()
    }

    private fun endOfDay(date: LocalDate): LocalDateTime {
        return date.plusDays(1).atStartOfDay()
    }

    override fun countAllUsers(): Long? {
        return em!!.createQuery<Long?>("select count(m) from Member m", Long::class.java)
            .getSingleResult()
    }

    override fun countAllPosts(): Long? {
        return em!!.createQuery<Long?>("select count(p) from Post p", Long::class.java)
            .getSingleResult()
    }

    override fun countPendingReports(): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(r)
                from Report r
                where r.status = 'PENDING'
                
                """.trimIndent(), Long::class.java
        )
            .getSingleResult()
    }

    override fun countTodayUsers(today: LocalDate): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(m)
                from Member m
                where m.createdAt >= :start and m.createdAt < :end
                
                """.trimIndent(), Long::class.java
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getSingleResult()
    }

    override fun countTodayPosts(today: LocalDate): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(p)
                from Post p
                where p.createdAt >= :start and p.createdAt < :end
                
                """.trimIndent(), Long::class.java
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getSingleResult()
    }

    override fun countTodayComments(today: LocalDate): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(c)
                from Comment c
                where c.createdAt >= :start and c.createdAt < :end
                
                """.trimIndent(), Long::class.java
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getSingleResult()
    }

    // ============================
    // Post Report
    // ============================
    override fun countTodayPostReports(today: LocalDate): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(r)
                from Report r
                where r.targetType = 'POST'
                and r.createdAt >= :start and r.createdAt < :end
                
                """.trimIndent(), Long::class.java
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getSingleResult()
    }

    override fun countTodayPostReportsByStatus(today: LocalDate, status: ReportStatus?): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(r)
                from Report r
                where r.targetType = 'POST'
                and r.status = :status
                and r.createdAt >= :start and r.createdAt < :end
                
                """.trimIndent(), Long::class.java
        )
            .setParameter("status", status)
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getSingleResult()
    }

    override fun countTodayPostReportsByReason(today: LocalDate): MutableList<ReportReasonCount?>? {
        return em!!.createQuery<ReportReasonCount?>(
            """
                select new com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto${'$'}ReportReasonCount(
                    r.reasonType, count(r)
                )
                from Report r
                where r.targetType = 'POST'
                and r.createdAt >= :start and r.createdAt < :end
                group by r.reasonType
                order by count(r) desc
                
                """.trimIndent(), ReportReasonCount::class.java
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getResultList()
    }

    // ============================
    // Comment Report
    // ============================
    override fun countTodayCommentReports(today: LocalDate): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(r)
                from Report r
                where r.targetType = 'COMMENT'
                and r.createdAt >= :start and r.createdAt < :end
                
                """.trimIndent(), Long::class.java
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getSingleResult()
    }

    override fun countTodayCommentReportsByStatus(today: LocalDate, status: ReportStatus?): Long? {
        return em!!.createQuery<Long?>(
            """
                select count(r)
                from Report r
                where r.targetType = 'COMMENT'
                and r.status = :status
                and r.createdAt >= :start and r.createdAt < :end
                
                """.trimIndent(), Long::class.java
        )
            .setParameter("status", status)
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getSingleResult()
    }

    override fun countTodayCommentReportsByReason(today: LocalDate): MutableList<ReportReasonCount?>? {
        return em!!.createQuery<ReportReasonCount?>(
            """
                select new com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto${'$'}ReportReasonCount(
                    r.reasonType, count(r)
                )
                from Report r
                where r.targetType = 'COMMENT'
                and r.createdAt >= :start and r.createdAt < :end
                group by r.reasonType
                order by count(r) desc
                
                """.trimIndent(), ReportReasonCount::class.java
        )
            .setParameter("start", startOfDay(today))
            .setParameter("end", endOfDay(today))
            .getResultList()
    }
}