package com.back.devc.domain.interaction.report.integration

import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.repository.ReportRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.List
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

internal object ReportGroupPagingBenchmark {
    fun measureOffsetPages(
        reportRepository: ReportRepository,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        pageSize: Int
    ): MutableList<PageMeasurement?> {
        return measureOffsetPages(
            reportRepository,
            status,
            from,
            to,
            BenchmarkOptions.Companion.defaultOffsetCheckpoints(pageSize)
        )
    }

    fun measureOffsetPages(
        reportRepository: ReportRepository,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        options: BenchmarkOptions
    ): MutableList<PageMeasurement?> {
        return options.offsetRows!!.stream()
            .map<PageMeasurement?> { offsetRows: Int? ->
                ReportGroupPagingBenchmark.measureRepeatedly(
                    MeasurementMode.PAGE_WITH_COUNT,
                    offsetRows!!,
                    options,
                    Supplier {
                        ReportGroupPagingBenchmark.runPageWithCount(
                            reportRepository,
                            status,
                            from,
                            to,
                            offsetRows,
                            options.pageSize
                        )
                    }
                )
            }
            .toList()
    }

    fun measureContentOnlyOffsetPages(
        entityManager: EntityManager,
        status: ReportStatus?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        options: BenchmarkOptions
    ): MutableList<PageMeasurement?> {
        return options.offsetRows!!.stream()
            .map<PageMeasurement?> { offsetRows: Int? ->
                ReportGroupPagingBenchmark.measureRepeatedly(
                    MeasurementMode.CONTENT_ONLY,
                    offsetRows!!,
                    options,
                    Supplier {
                        ReportGroupPagingBenchmark.runContentOnly(
                            entityManager,
                            status,
                            from,
                            to,
                            offsetRows,
                            options.pageSize
                        )
                    }
                )
            }
            .toList()
    }

    fun explainGroupedContentQuery(
        entityManager: EntityManager,
        status: ReportStatus?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        offsetRows: Int,
        pageSize: Int
    ): MutableList<String?> {
        val query = entityManager.createNativeQuery(
            """
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
                
                """.trimIndent()
        )
        val statusName = if (status == null) null else status.name
        query.setParameter(1, statusName)
        query.setParameter(2, statusName)
        query.setParameter(3, from)
        query.setParameter(4, to)
        query.setParameter(5, pageSize)
        query.setParameter(6, offsetRows)

        return (query.getResultList() as MutableList<Array<Any?>?>).stream()
            .map<String?> { a: Array<Any?>? -> a.contentToString() }
            .toList()
    }

    private fun measureRepeatedly(
        mode: MeasurementMode?,
        offsetRows: Int,
        options: BenchmarkOptions,
        run: Supplier<SingleRunMeasurement?>
    ): PageMeasurement {
        for (i in 0..<options.warmupRuns) {
            run.get()
        }

        var sumElapsedNanos: Long = 0
        var minElapsedNanos = Long.Companion.MAX_VALUE
        var maxElapsedNanos = Long.Companion.MIN_VALUE
        var lastRun: SingleRunMeasurement? = null

        for (i in 0..<options.measurementRuns) {
            lastRun = run.get()
            val elapsedNanos = lastRun!!.elapsedNanos
            sumElapsedNanos += elapsedNanos
            minElapsedNanos = min(minElapsedNanos, elapsedNanos)
            maxElapsedNanos = max(maxElapsedNanos, elapsedNanos)
        }

        requireNotNull(lastRun) { "measurementRuns must be greater than 0" }

        return PageMeasurement(
            mode,
            offsetRows,
            offsetRows / options.pageSize,
            options.pageSize,
            lastRun.contentSize,
            lastRun.totalElements,
            lastRun.totalPages,
            options.warmupRuns,
            options.measurementRuns,
            sumElapsedNanos / options.measurementRuns,
            minElapsedNanos,
            maxElapsedNanos
        )
    }

    private fun runPageWithCount(
        reportRepository: ReportRepository,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        offsetRows: Int,
        pageSize: Int
    ): SingleRunMeasurement {
        val pageNumber = offsetRows / pageSize
        val startedAt = System.nanoTime()
        val page: Page<Array<Any?>?> = reportRepository.findGroupedReports(
            status,
            from,
            to,
            PageRequest.of(pageNumber, pageSize)
        )
        val elapsedNanos = System.nanoTime() - startedAt

        return SingleRunMeasurement(
            page.getNumberOfElements(),
            page.getTotalElements(),
            page.getTotalPages(),
            elapsedNanos
        )
    }

    private fun runContentOnly(
        entityManager: EntityManager,
        status: ReportStatus?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        offsetRows: Int,
        pageSize: Int
    ): SingleRunMeasurement {
        val startedAt = System.nanoTime()
        val rows = entityManager.createQuery(
            """
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
                        
                        """.trimIndent()
        )
            .setParameter("status", status)
            .setParameter("from", from)
            .setParameter("to", to)
            .setFirstResult(offsetRows)
            .setMaxResults(pageSize)
            .getResultList()
        val elapsedNanos = System.nanoTime() - startedAt

        return SingleRunMeasurement(
            rows.size,
            -1L,
            -1,
            elapsedNanos
        )
    }

    internal class BenchmarkOptions(
        val pageSize: Int,
        val warmupRuns: Int,
        val measurementRuns: Int,
        offsetRows: MutableList<Int?>?
    ) {
        val offsetRows: MutableList<Int?>?

        init {
            var offsetRows = offsetRows
            require(pageSize > 0) { "pageSize must be greater than 0" }
            require(warmupRuns >= 0) { "warmupRuns must be greater than or equal to 0" }
            require(measurementRuns > 0) { "measurementRuns must be greater than 0" }
            require(!(offsetRows == null || offsetRows.isEmpty())) { "offsetRows must not be empty" }
            require(
                !offsetRows.stream()
                    .anyMatch { offset: Int? -> offset!! < 0 || offset % pageSize != 0 }) { "offsetRows must be non-negative multiples of pageSize" }
            offsetRows = List.copyOf<Int?>(offsetRows)
            this.offsetRows = offsetRows
        }

        companion object {
            fun defaultOffsetCheckpoints(pageSize: Int): BenchmarkOptions {
                return BenchmarkOptions(
                    pageSize,
                    2,
                    3,
                    mutableListOf<Int?>(0, 100, 1000, 10000, 50000)
                )
            }
        }
    }

    internal enum class MeasurementMode {
        PAGE_WITH_COUNT,
        CONTENT_ONLY
    }

    @JvmRecord
    internal data class PageMeasurement(
        val mode: MeasurementMode?,
        val offsetRows: Int,
        val pageNumber: Int,
        val pageSize: Int,
        val contentSize: Int,
        val totalElements: Long,
        val totalPages: Int,
        val warmupRuns: Int,
        val measurementRuns: Int,
        val avgElapsedNanos: Long,
        val minElapsedNanos: Long,
        val maxElapsedNanos: Long
    ) {
        fun avgElapsedMillis(): Long {
            return avgElapsedNanos / 1000000
        }

        fun minElapsedMillis(): Long {
            return minElapsedNanos / 1000000
        }

        fun maxElapsedMillis(): Long {
            return maxElapsedNanos / 1000000
        }
    }

    @JvmRecord
    private data class SingleRunMeasurement(
        val contentSize: Int,
        val totalElements: Long,
        val totalPages: Int,
        val elapsedNanos: Long
    )
}
