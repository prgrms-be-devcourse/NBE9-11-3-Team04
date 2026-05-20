package com.back.devc.domain.interaction.report.integration

import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.Report.Companion.create
import com.back.devc.domain.interaction.report.entity.ReportGroup
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.interaction.report.integration.ReportGroupPagingBenchmark.*
import com.back.devc.domain.interaction.report.repository.ReportGroupRepository
import com.back.devc.domain.interaction.report.repository.ReportRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalMember
import com.back.devc.domain.member.member.repository.MemberRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.function.Consumer
import java.util.function.LongFunction
import java.util.stream.LongStream

@DataJpaTest
@ActiveProfiles("test")
internal class ReportGroupPagingBenchmarkTest {
    @Autowired
    private val reportRepository: ReportRepository? = null

    @Autowired
    private val reportGroupRepository: ReportGroupRepository? = null

    @Autowired
    private val memberRepository: MemberRepository? = null

    @PersistenceContext
    private val entityManager: EntityManager? = null

    @Test
    fun benchmarkMeasuresOffsetCheckpointsWithWarmupAndRepeatedRuns() {
        val reporter = memberRepository!!.save<Member>(
            createLocalMember(
                "benchmark-reporter@test.com",
                "password123!",
                "benchmarkReporter"
            )
        )

        for (targetId in 1..25) {
            reportRepository!!.save<Report?>(report(reporter, targetId))
        }

        val options =
            BenchmarkOptions(
                10,
                2,
                3,
                mutableListOf<Int?>(0, 10, 20)
            )

        val measurements =
            ReportGroupPagingBenchmark.measureOffsetPages(
                reportRepository,
                ReportStatus.PENDING,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                options
            )

        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<MeasurementMode?, RuntimeException?>(PageMeasurement::mode)
            .containsOnly(MeasurementMode.PAGE_WITH_COUNT)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Int?, RuntimeException?>(PageMeasurement::offsetRows)
            .containsExactly(0, 10, 20)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Int?, RuntimeException?>(PageMeasurement::pageNumber)
            .containsExactly(0, 1, 2)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Long?, RuntimeException?>(PageMeasurement::totalElements)
            .containsOnly(25L)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Int?, RuntimeException?>(PageMeasurement::contentSize)
            .containsExactly(10, 10, 5)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .allSatisfy(ThrowingConsumer { measurement: PageMeasurement? ->
                Assertions.assertThat(measurement!!.warmupRuns).isEqualTo(2)
                Assertions.assertThat(measurement.measurementRuns).isEqualTo(3)
                Assertions.assertThat(measurement.minElapsedNanos).isLessThanOrEqualTo(measurement.avgElapsedNanos)
                Assertions.assertThat(measurement.avgElapsedNanos).isLessThanOrEqualTo(measurement.maxElapsedNanos)
            })
    }

    @Test
    fun benchmarkMeasuresContentOnlyWithoutPageCount() {
        val reporter = memberRepository!!.save<Member>(
            createLocalMember(
                "content-only-reporter@test.com",
                "password123!",
                "contentOnlyReporter"
            )
        )

        for (targetId in 1..25) {
            reportRepository!!.save<Report?>(report(reporter, targetId))
        }

        val options =
            BenchmarkOptions(
                10,
                1,
                2,
                mutableListOf<Int?>(0, 10, 20)
            )

        val measurements =
            ReportGroupPagingBenchmark.measureContentOnlyOffsetPages(
                entityManager,
                ReportStatus.PENDING,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                options
            )

        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<MeasurementMode?, RuntimeException?>(PageMeasurement::mode)
            .containsOnly(MeasurementMode.CONTENT_ONLY)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Int?, RuntimeException?>(PageMeasurement::offsetRows)
            .containsExactly(0, 10, 20)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Long?, RuntimeException?>(PageMeasurement::totalElements)
            .containsOnly(-1L)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Int?, RuntimeException?>(PageMeasurement::totalPages)
            .containsOnly(-1)
        Assertions.assertThat<PageMeasurement?>(measurements)
            .extracting<Int?, RuntimeException?>(PageMeasurement::contentSize)
            .containsExactly(10, 10, 5)
    }

    private fun report(reporter: Member, targetId: Long): Report {
        val report = create(
            reporter,
            TargetType.POST,
            targetId,
            "SPAM",
            "benchmark"
        )
        report.assignReportGroup(reportGroup(targetId))
        return report
    }

    private fun reportGroup(targetId: Long): ReportGroup {
        val reportedAt = LocalDateTime.now()

        var reportGroup = reportGroupRepository!!
            .findByTargetTypeAndTargetId(TargetType.POST, targetId)

        if (reportGroup == null) {
            reportGroup = ReportGroup(TargetType.POST, targetId, reportedAt)
        }

        reportGroup.registerReport(reportedAt)
        return reportGroupRepository.saveAndFlush<ReportGroup>(reportGroup)
    }

    @Test
    @Disabled("Local benchmark only. Enable manually with -Dreport.benchmark.groups=500000 for million-row style data.")
    fun measureLargeOffsetPages() {
        val groupCount = Integer.getInteger("report.benchmark.groups", 10000)
        val reportsPerGroup = Integer.getInteger("report.benchmark.reportsPerGroup", 2)
        val pageSize = Integer.getInteger("report.benchmark.pageSize", 20)
        val options: BenchmarkOptions =
            BenchmarkOptions.Companion.defaultOffsetCheckpoints(pageSize)

        val reporters = createReporters(reportsPerGroup)
        seedReports(groupCount, reporters)

        val pageMeasurements =
            ReportGroupPagingBenchmark.measureOffsetPages(
                reportRepository,
                ReportStatus.PENDING,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                options
            )

        val contentOnlyMeasurements =
            ReportGroupPagingBenchmark.measureContentOnlyOffsetPages(
                entityManager,
                ReportStatus.PENDING,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                options
            )

        pageMeasurements.forEach(Consumer { measurement: PageMeasurement? -> this.printMeasurement(measurement!!) })
        contentOnlyMeasurements.forEach(Consumer { measurement: PageMeasurement? -> this.printMeasurement(measurement!!) })

        ReportGroupPagingBenchmark.explainGroupedContentQuery(
            entityManager,
            ReportStatus.PENDING,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
            50000,
            pageSize
        ).forEach(Consumer { x: String? -> println(x) })
    }

    private fun printMeasurement(measurement: PageMeasurement) {
        System.out.printf(
            "%s offset=%d page=%d size=%d content=%d totalElements=%d totalPages=%d warmups=%d runs=%d avgMillis=%d minMillis=%d maxMillis=%d%n",
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
    }

    private fun createReporters(reportsPerGroup: Int): MutableList<Member> {
        return LongStream.rangeClosed(1, reportsPerGroup.toLong())
            .mapToObj<Member>(LongFunction { i: Long ->
                createLocalMember(
                    "benchmark-reporter-" + i + "@test.com",
                    "password123!",
                    "benchmarkReporter" + i
                )
            })
            .map<Member> { entity: Member? -> memberRepository!!.save(entity) }
            .toList()
    }

    private fun seedReports(groupCount: Int, reporters: MutableList<Member>) {
        var persisted = 0

        for (targetId in 1..groupCount) {
            for (reporter in reporters) {
                entityManager!!.persist(report(reporter, targetId))
                persisted++

                if (persisted % 1000 == 0) {
                    entityManager.flush()
                    entityManager.clear()
                }
            }
        }

        entityManager!!.flush()
        entityManager.clear()
    }
}
