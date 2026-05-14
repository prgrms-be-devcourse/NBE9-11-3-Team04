package com.back.devc.domain.admin.dashboard.repository;

import com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto.ReportReasonCount;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminDashboardRepositoryImpl implements AdminDashboardRepository {

    private final EntityManager em;

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    @Override
    public Long countAllUsers() {
        return em.createQuery("select count(m) from Member m", Long.class)
                .getSingleResult();
    }

    @Override
    public Long countAllPosts() {
        return em.createQuery("select count(p) from Post p", Long.class)
                .getSingleResult();
    }

    @Override
    public Long countPendingReports() {
        return em.createQuery("""
                select count(r)
                from Report r
                where r.status = 'PENDING'
                """, Long.class)
                .getSingleResult();
    }

    @Override
    public Long countTodayUsers(LocalDate today) {
        return em.createQuery("""
                select count(m)
                from Member m
                where m.createdAt >= :start and m.createdAt < :end
                """, Long.class)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getSingleResult();
    }

    @Override
    public Long countTodayPosts(LocalDate today) {
        return em.createQuery("""
                select count(p)
                from Post p
                where p.createdAt >= :start and p.createdAt < :end
                """, Long.class)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getSingleResult();
    }

    @Override
    public Long countTodayComments(LocalDate today) {
        return em.createQuery("""
                select count(c)
                from Comment c
                where c.createdAt >= :start and c.createdAt < :end
                """, Long.class)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getSingleResult();
    }

    // ============================
    // Post Report
    // ============================

    @Override
    public Long countTodayPostReports(LocalDate today) {
        return em.createQuery("""
                select count(r)
                from Report r
                where r.targetType = 'POST'
                and r.createdAt >= :start and r.createdAt < :end
                """, Long.class)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getSingleResult();
    }

    @Override
    public Long countTodayPostReportsByStatus(LocalDate today, ReportStatus status) {
        return em.createQuery("""
                select count(r)
                from Report r
                where r.targetType = 'POST'
                and r.status = :status
                and r.createdAt >= :start and r.createdAt < :end
                """, Long.class)
                .setParameter("status", status)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getSingleResult();
    }

    @Override
    public List<ReportReasonCount> countTodayPostReportsByReason(LocalDate today) {
        return em.createQuery("""
                select new com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto$ReportReasonCount(
                    r.reasonType, count(r)
                )
                from Report r
                where r.targetType = 'POST'
                and r.createdAt >= :start and r.createdAt < :end
                group by r.reasonType
                order by count(r) desc
                """, ReportReasonCount.class)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getResultList();
    }

    // ============================
    // Comment Report
    // ============================

    @Override
    public Long countTodayCommentReports(LocalDate today) {
        return em.createQuery("""
                select count(r)
                from Report r
                where r.targetType = 'COMMENT'
                and r.createdAt >= :start and r.createdAt < :end
                """, Long.class)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getSingleResult();
    }

    @Override
    public Long countTodayCommentReportsByStatus(LocalDate today, ReportStatus status) {
        return em.createQuery("""
                select count(r)
                from Report r
                where r.targetType = 'COMMENT'
                and r.status = :status
                and r.createdAt >= :start and r.createdAt < :end
                """, Long.class)
                .setParameter("status", status)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getSingleResult();
    }

    @Override
    public List<ReportReasonCount> countTodayCommentReportsByReason(LocalDate today) {
        return em.createQuery("""
                select new com.back.devc.domain.admin.dashboard.dto.DashboardResponseDto$ReportReasonCount(
                    r.reasonType, count(r)
                )
                from Report r
                where r.targetType = 'COMMENT'
                and r.createdAt >= :start and r.createdAt < :end
                group by r.reasonType
                order by count(r) desc
                """, ReportReasonCount.class)
                .setParameter("start", startOfDay(today))
                .setParameter("end", endOfDay(today))
                .getResultList();
    }
}