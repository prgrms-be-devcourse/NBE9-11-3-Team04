package com.back.devc.interaction.report.integration

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.interaction.report.integration.ReportGroupPagingBenchmark.BenchmarkOptions
import com.back.devc.interaction.report.integration.ReportGroupPagingBenchmark.MeasurementMode
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
internal class ReportGroupPagingBenchmarkTest @Autowired constructor(
    private val reportRepository: ReportRepository,
    private val reportGroupRepository: ReportGroupRepository,
    private val memberRepository: MemberRepository,
    private val entityManager: EntityManager
) {

    @Test
    fun benchmarkMeasuresOffsetCheckpointsWithWarmupAndRepeatedRuns() {
        val reporter = saveMember("benchmark-reporter@test.com", "benchmarkReporter")
        seedReportsForTargets(reporter, 1L..25L)
        val options = BenchmarkOptions(
            pageSize = 10,
            warmupRuns = 2,
            measurementRuns = 3,
            offsetRows = listOf(0, 10, 20)
        )

        val measurements = ReportGroupPagingBenchmark.measureOffsetPages(
            reportRepository = reportRepository,
            status = ReportStatus.PENDING,
            from = LocalDateTime.now().minusDays(1),
            to = LocalDateTime.now().plusDays(1),
            options = options
        )

        assertThat(measurements.map { it.mode }).containsOnly(MeasurementMode.PAGE_WITH_COUNT)
        assertThat(measurements.map { it.offsetRows }).containsExactly(0, 10, 20)
        assertThat(measurements.map { it.pageNumber }).containsExactly(0, 1, 2)
        assertThat(measurements.map { it.totalElements }).containsOnly(25L)
        assertThat(measurements.map { it.contentSize }).containsExactly(10, 10, 5)
        assertThat(measurements).allSatisfy { measurement ->
            assertThat(measurement.warmupRuns).isEqualTo(2)
            assertThat(measurement.measurementRuns).isEqualTo(3)
            assertThat(measurement.minElapsedNanos).isLessThanOrEqualTo(measurement.avgElapsedNanos)
            assertThat(measurement.avgElapsedNanos).isLessThanOrEqualTo(measurement.maxElapsedNanos)
        }
    }

    @Test
    fun benchmarkMeasuresContentOnlyWithoutPageCount() {
        val reporter = saveMember("content-only-reporter@test.com", "contentOnlyReporter")
        seedReportsForTargets(reporter, 1L..25L)
        val options = BenchmarkOptions(
            pageSize = 10,
            warmupRuns = 1,
            measurementRuns = 2,
            offsetRows = listOf(0, 10, 20)
        )

        val measurements = ReportGroupPagingBenchmark.measureContentOnlyOffsetPages(
            entityManager = entityManager,
            status = ReportStatus.PENDING,
            from = LocalDateTime.now().minusDays(1),
            to = LocalDateTime.now().plusDays(1),
            options = options
        )

        assertThat(measurements.map { it.mode }).containsOnly(MeasurementMode.CONTENT_ONLY)
        assertThat(measurements.map { it.offsetRows }).containsExactly(0, 10, 20)
        assertThat(measurements.map { it.totalElements }).containsOnly(-1L)
        assertThat(measurements.map { it.totalPages }).containsOnly(-1)
        assertThat(measurements.map { it.contentSize }).containsExactly(10, 10, 5)
    }

    @Test
    @Disabled("Local benchmark only. Enable manually with -Dreport.benchmark.groups=500000 for million-row style data.")
    fun measureLargeOffsetPages() {
        val groupCount = Integer.getInteger("report.benchmark.groups", 10_000)
        val reportsPerGroup = Integer.getInteger("report.benchmark.reportsPerGroup", 2)
        val pageSize = Integer.getInteger("report.benchmark.pageSize", 20)
        val options = BenchmarkOptions.defaultOffsetCheckpoints(pageSize)

        val reporters = createReporters(reportsPerGroup)
        seedReports(groupCount, reporters)

        val from = LocalDateTime.now().minusDays(1)
        val to = LocalDateTime.now().plusDays(1)

        val pageMeasurements = ReportGroupPagingBenchmark.measureOffsetPages(
            reportRepository = reportRepository,
            status = ReportStatus.PENDING,
            from = from,
            to = to,
            options = options
        )
        val contentOnlyMeasurements = ReportGroupPagingBenchmark.measureContentOnlyOffsetPages(
            entityManager = entityManager,
            status = ReportStatus.PENDING,
            from = from,
            to = to,
            options = options
        )

        pageMeasurements.forEach(::printMeasurement)
        contentOnlyMeasurements.forEach(::printMeasurement)
        ReportGroupPagingBenchmark.explainGroupedContentQuery(
            entityManager = entityManager,
            status = ReportStatus.PENDING,
            from = from,
            to = to,
            offsetRows = 50_000,
            pageSize = pageSize
        ).forEach(::println)
    }

    private fun seedReportsForTargets(reporter: Member, targetIds: LongRange) {
        targetIds.forEach { targetId ->
            reportRepository.save(report(reporter, targetId))
        }
    }

    private fun saveMember(email: String, nickname: String): Member =
        memberRepository.save(
            Member.createLocalMember(
                email = email,
                passwordHash = "password123!",
                nickname = nickname
            )
        )

    private fun report(reporter: Member, targetId: Long): Report {
        val report = Report.create(
            reporter = reporter,
            targetType = TargetType.POST,
            targetId = targetId,
            reasonType = "SPAM",
            reasonDetail = "benchmark"
        )
        report.assignReportGroup(reportGroup(targetId))
        return report
    }

    private fun reportGroup(targetId: Long): ReportGroup {
        val reportedAt = LocalDateTime.now()
        val reportGroup = reportGroupRepository.findByTargetTypeAndTargetId(TargetType.POST, targetId)
            ?: ReportGroup(TargetType.POST, targetId, reportedAt)

        reportGroup.registerReport(reportedAt)
        return reportGroupRepository.saveAndFlush(reportGroup)
    }

    private fun printMeasurement(measurement: ReportGroupPagingBenchmark.PageMeasurement) {
        println(
            "%s offset=%d page=%d size=%d content=%d totalElements=%d totalPages=%d warmups=%d runs=%d avgMillis=%d minMillis=%d maxMillis=%d".format(
                measurement.mode,
                measurement.offsetRows,
                measurement.pageNumber,
                measurement.pageSize,
                measurement.contentSize,
                measurement.totalElements,
                measurement.totalPages,
                measurement.warmupRuns,
                measurement.measurementRuns,
                measurement.avgElapsedMillis(),
                measurement.minElapsedMillis(),
                measurement.maxElapsedMillis()
            )
        )
    }

    private fun createReporters(reportsPerGroup: Int): List<Member> =
        (1..reportsPerGroup).map { index ->
            saveMember(
                email = "benchmark-reporter-$index@test.com",
                nickname = "benchmarkReporter$index"
            )
        }

    private fun seedReports(groupCount: Int, reporters: List<Member>) {
        var persisted = 0

        for (targetId in 1L..groupCount.toLong()) {
            reporters.forEach { reporter ->
                entityManager.persist(report(reporter, targetId))
                persisted++

                if (persisted % 1_000 == 0) {
                    entityManager.flush()
                    entityManager.clear()
                }
            }
        }

        entityManager.flush()
        entityManager.clear()
    }
}
