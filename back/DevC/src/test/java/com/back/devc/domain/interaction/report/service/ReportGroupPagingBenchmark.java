package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

final class ReportGroupPagingBenchmark {

    private ReportGroupPagingBenchmark() {
    }

    static List<PageMeasurement> measureOffsetPages(
            ReportRepository reportRepository,
            ReportStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int pageSize
    ) {
        return measureOffsetPages(
                reportRepository,
                status,
                from,
                to,
                BenchmarkOptions.defaultOffsetCheckpoints(pageSize)
        );
    }

    static List<PageMeasurement> measureOffsetPages(
            ReportRepository reportRepository,
            ReportStatus status,
            LocalDateTime from,
            LocalDateTime to,
            BenchmarkOptions options
    ) {
        return options.offsetRows().stream()
                .map(offsetRows -> measureRepeatedly(
                        MeasurementMode.PAGE_WITH_COUNT,
                        offsetRows,
                        options,
                        () -> runPageWithCount(reportRepository, status, from, to, offsetRows, options.pageSize())
                ))
                .toList();
    }

    static List<PageMeasurement> measureContentOnlyOffsetPages(
            EntityManager entityManager,
            ReportStatus status,
            LocalDateTime from,
            LocalDateTime to,
            BenchmarkOptions options
    ) {
        return options.offsetRows().stream()
                .map(offsetRows -> measureRepeatedly(
                        MeasurementMode.CONTENT_ONLY,
                        offsetRows,
                        options,
                        () -> runContentOnly(entityManager, status, from, to, offsetRows, options.pageSize())
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    static List<String> explainGroupedContentQuery(
            EntityManager entityManager,
            ReportStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int offsetRows,
            int pageSize
    ) {
        Query query = entityManager.createNativeQuery("""
                EXPLAIN SELECT target_type,
                               target_id,
                               COUNT(*) AS report_count,
                               MAX(created_at) AS latest_created_at
                FROM reports
                WHERE (?1 IS NULL OR status = ?2)
                  AND created_at >= ?3
                  AND created_at < ?4
                GROUP BY target_type, target_id
                ORDER BY latest_created_at DESC, target_type ASC, target_id DESC
                LIMIT ?5 OFFSET ?6
                """);
        String statusName = status == null ? null : status.name();
        query.setParameter(1, statusName);
        query.setParameter(2, statusName);
        query.setParameter(3, from);
        query.setParameter(4, to);
        query.setParameter(5, pageSize);
        query.setParameter(6, offsetRows);

        return ((List<Object[]>) query.getResultList()).stream()
                .map(java.util.Arrays::toString)
                .toList();
    }

    private static PageMeasurement measureRepeatedly(
            MeasurementMode mode,
            int offsetRows,
            BenchmarkOptions options,
            Supplier<SingleRunMeasurement> run
    ) {
        for (int i = 0; i < options.warmupRuns(); i++) {
            run.get();
        }

        long sumElapsedNanos = 0;
        long minElapsedNanos = Long.MAX_VALUE;
        long maxElapsedNanos = Long.MIN_VALUE;
        SingleRunMeasurement lastRun = null;

        for (int i = 0; i < options.measurementRuns(); i++) {
            lastRun = run.get();
            long elapsedNanos = lastRun.elapsedNanos();
            sumElapsedNanos += elapsedNanos;
            minElapsedNanos = Math.min(minElapsedNanos, elapsedNanos);
            maxElapsedNanos = Math.max(maxElapsedNanos, elapsedNanos);
        }

        if (lastRun == null) {
            throw new IllegalArgumentException("measurementRuns must be greater than 0");
        }

        return new PageMeasurement(
                mode,
                offsetRows,
                offsetRows / options.pageSize(),
                options.pageSize(),
                lastRun.contentSize(),
                lastRun.totalElements(),
                lastRun.totalPages(),
                options.warmupRuns(),
                options.measurementRuns(),
                sumElapsedNanos / options.measurementRuns(),
                minElapsedNanos,
                maxElapsedNanos
        );
    }

    private static SingleRunMeasurement runPageWithCount(
            ReportRepository reportRepository,
            ReportStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int offsetRows,
            int pageSize
    ) {
        int pageNumber = offsetRows / pageSize;
        long startedAt = System.nanoTime();
        Page<Object[]> page = reportRepository.findGroupedReports(
                status,
                from,
                to,
                PageRequest.of(pageNumber, pageSize)
        );
        long elapsedNanos = System.nanoTime() - startedAt;

        return new SingleRunMeasurement(
                page.getNumberOfElements(),
                page.getTotalElements(),
                page.getTotalPages(),
                elapsedNanos
        );
    }

    private static SingleRunMeasurement runContentOnly(
            EntityManager entityManager,
            ReportStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int offsetRows,
            int pageSize
    ) {
        long startedAt = System.nanoTime();
        List<?> rows = entityManager.createQuery("""
                        SELECT r.targetType,
                               r.targetId,
                               COUNT(r),
                               MAX(r.createdAt)
                        FROM Report r
                        WHERE (:status IS NULL OR r.status = :status)
                          AND r.createdAt >= :from
                          AND r.createdAt < :to
                        GROUP BY r.targetType, r.targetId
                        ORDER BY MAX(r.createdAt) DESC, r.targetType ASC, r.targetId DESC
                        """)
                .setParameter("status", status)
                .setParameter("from", from)
                .setParameter("to", to)
                .setFirstResult(offsetRows)
                .setMaxResults(pageSize)
                .getResultList();
        long elapsedNanos = System.nanoTime() - startedAt;

        return new SingleRunMeasurement(
                rows.size(),
                -1L,
                -1,
                elapsedNanos
        );
    }

    record BenchmarkOptions(
            int pageSize,
            int warmupRuns,
            int measurementRuns,
            List<Integer> offsetRows
    ) {

        BenchmarkOptions {
            if (pageSize <= 0) {
                throw new IllegalArgumentException("pageSize must be greater than 0");
            }
            if (warmupRuns < 0) {
                throw new IllegalArgumentException("warmupRuns must be greater than or equal to 0");
            }
            if (measurementRuns <= 0) {
                throw new IllegalArgumentException("measurementRuns must be greater than 0");
            }
            if (offsetRows == null || offsetRows.isEmpty()) {
                throw new IllegalArgumentException("offsetRows must not be empty");
            }
            if (offsetRows.stream().anyMatch(offset -> offset < 0 || offset % pageSize != 0)) {
                throw new IllegalArgumentException("offsetRows must be non-negative multiples of pageSize");
            }
            offsetRows = List.copyOf(offsetRows);
        }

        static BenchmarkOptions defaultOffsetCheckpoints(int pageSize) {
            return new BenchmarkOptions(
                    pageSize,
                    2,
                    3,
                    List.of(0, 100, 1_000, 10_000, 50_000)
            );
        }
    }

    enum MeasurementMode {
        PAGE_WITH_COUNT,
        CONTENT_ONLY
    }

    record PageMeasurement(
            MeasurementMode mode,
            int offsetRows,
            int pageNumber,
            int pageSize,
            int contentSize,
            long totalElements,
            int totalPages,
            int warmupRuns,
            int measurementRuns,
            long avgElapsedNanos,
            long minElapsedNanos,
            long maxElapsedNanos
    ) {

        long avgElapsedMillis() {
            return avgElapsedNanos / 1_000_000;
        }

        long minElapsedMillis() {
            return minElapsedNanos / 1_000_000;
        }

        long maxElapsedMillis() {
            return maxElapsedNanos / 1_000_000;
        }
    }

    private record SingleRunMeasurement(
            int contentSize,
            long totalElements,
            int totalPages,
            long elapsedNanos
    ) {
    }
}
