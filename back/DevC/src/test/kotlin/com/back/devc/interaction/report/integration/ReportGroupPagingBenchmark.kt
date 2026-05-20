package com.back.devc.interaction.report.integration

import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.repository.ReportRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min

internal object ReportGroupPagingBenchmark {
    fun measureOffsetPages(
        reportRepository: ReportRepository,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        pageSize: Int
    ): List<PageMeasurement> =
        measureOffsetPages(
            reportRepository = reportRepository,
            status = status,
            from = from,
            to = to,
            options = BenchmarkOptions.defaultOffsetCheckpoints(pageSize)
        )

    fun measureOffsetPages(
        reportRepository: ReportRepository,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        options: BenchmarkOptions
    ): List<PageMeasurement> =
        options.offsetRows.map { offsetRows ->
            measureRepeatedly(
                mode = MeasurementMode.PAGE_WITH_COUNT,
                offsetRows = offsetRows,
                options = options
            ) {
                runPageWithCount(
                    reportRepository = reportRepository,
                    status = status,
                    from = from,
                    to = to,
                    offsetRows = offsetRows,
                    pageSize = options.pageSize
                )
            }
        }

    fun measureContentOnlyOffsetPages(
        entityManager: EntityManager,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        options: BenchmarkOptions
    ): List<PageMeasurement> =
        options.offsetRows.map { offsetRows ->
            measureRepeatedly(
                mode = MeasurementMode.CONTENT_ONLY,
                offsetRows = offsetRows,
                options = options
            ) {
                runContentOnly(
                    entityManager = entityManager,
                    status = status,
                    from = from,
                    to = to,
                    offsetRows = offsetRows,
                    pageSize = options.pageSize
                )
            }
        }

    fun explainGroupedContentQuery(
        entityManager: EntityManager,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
        offsetRows: Int,
        pageSize: Int
    ): List<String> {
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
        val statusName = status?.name
        query.setParameter(1, statusName)
        query.setParameter(2, statusName)
        query.setParameter(3, from)
        query.setParameter(4, to)
        query.setParameter(5, pageSize)
        query.setParameter(6, offsetRows)

        return query.resultList.map { row -> row.toString() }
    }

    private fun measureRepeatedly(
        mode: MeasurementMode,
        offsetRows: Int,
        options: BenchmarkOptions,
        run: () -> SingleRunMeasurement
    ): PageMeasurement {
        repeat(options.warmupRuns) {
            run()
        }

        var sumElapsedNanos = 0L
        var minElapsedNanos = Long.MAX_VALUE
        var maxElapsedNanos = Long.MIN_VALUE
        var lastRun: SingleRunMeasurement? = null

        repeat(options.measurementRuns) {
            val currentRun = run()
            lastRun = currentRun
            sumElapsedNanos += currentRun.elapsedNanos
            minElapsedNanos = min(minElapsedNanos, currentRun.elapsedNanos)
            maxElapsedNanos = max(maxElapsedNanos, currentRun.elapsedNanos)
        }

        val measuredRun = lastRun ?: throw IllegalArgumentException("measurementRuns must be greater than 0")

        return PageMeasurement(
            mode = mode,
            offsetRows = offsetRows,
            pageNumber = offsetRows / options.pageSize,
            pageSize = options.pageSize,
            contentSize = measuredRun.contentSize,
            totalElements = measuredRun.totalElements,
            totalPages = measuredRun.totalPages,
            warmupRuns = options.warmupRuns,
            measurementRuns = options.measurementRuns,
            avgElapsedNanos = sumElapsedNanos / options.measurementRuns,
            minElapsedNanos = minElapsedNanos,
            maxElapsedNanos = maxElapsedNanos
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
        val page = reportRepository.findGroupedReports(
            status = status,
            from = from,
            to = to,
            pageable = PageRequest.of(pageNumber, pageSize)
        )
        val elapsedNanos = System.nanoTime() - startedAt

        return SingleRunMeasurement(
            contentSize = page.numberOfElements,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            elapsedNanos = elapsedNanos
        )
    }

    private fun runContentOnly(
        entityManager: EntityManager,
        status: ReportStatus?,
        from: LocalDateTime,
        to: LocalDateTime,
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
            .resultList
        val elapsedNanos = System.nanoTime() - startedAt

        return SingleRunMeasurement(
            contentSize = rows.size,
            totalElements = -1L,
            totalPages = -1,
            elapsedNanos = elapsedNanos
        )
    }

    internal data class BenchmarkOptions(
        val pageSize: Int,
        val warmupRuns: Int,
        val measurementRuns: Int,
        val offsetRows: List<Int>
    ) {
        init {
            require(pageSize > 0) { "pageSize must be greater than 0" }
            require(warmupRuns >= 0) { "warmupRuns must be greater than or equal to 0" }
            require(measurementRuns > 0) { "measurementRuns must be greater than 0" }
            require(offsetRows.isNotEmpty()) { "offsetRows must not be empty" }
            require(offsetRows.all { it >= 0 && it % pageSize == 0 }) {
                "offsetRows must be non-negative multiples of pageSize"
            }
        }

        companion object {
            fun defaultOffsetCheckpoints(pageSize: Int): BenchmarkOptions =
                BenchmarkOptions(
                    pageSize = pageSize,
                    warmupRuns = 2,
                    measurementRuns = 3,
                    offsetRows = listOf(0, 100, 1000, 10000, 50000)
                )
        }
    }

    internal enum class MeasurementMode {
        PAGE_WITH_COUNT,
        CONTENT_ONLY
    }

    internal data class PageMeasurement(
        val mode: MeasurementMode,
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
        fun avgElapsedMillis(): Long = avgElapsedNanos / 1_000_000
        fun minElapsedMillis(): Long = minElapsedNanos / 1_000_000
        fun maxElapsedMillis(): Long = maxElapsedNanos / 1_000_000
    }

    private data class SingleRunMeasurement(
        val contentSize: Int,
        val totalElements: Long,
        val totalPages: Int,
        val elapsedNanos: Long
    )
}
