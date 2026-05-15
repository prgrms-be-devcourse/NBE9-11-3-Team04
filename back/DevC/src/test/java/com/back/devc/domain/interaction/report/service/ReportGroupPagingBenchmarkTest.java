package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReportGroupPagingBenchmarkTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void benchmarkMeasuresFirstMiddleAndLastOffsetPages() {
        Member reporter = memberRepository.save(Member.createLocalMember(
                "benchmark-reporter@test.com",
                "password123!",
                "benchmarkReporter"
        ));

        for (long targetId = 1; targetId <= 25; targetId++) {
            reportRepository.save(report(reporter, targetId));
        }

        List<ReportGroupPagingBenchmark.PageMeasurement> measurements =
                ReportGroupPagingBenchmark.measureOffsetPages(
                        reportRepository,
                        ReportStatus.PENDING,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(1),
                        10
                );

        assertThat(measurements).extracting(ReportGroupPagingBenchmark.PageMeasurement::label)
                .containsExactly("first", "middle", "last");
        assertThat(measurements).extracting(ReportGroupPagingBenchmark.PageMeasurement::pageNumber)
                .containsExactly(0, 1, 2);
        assertThat(measurements).extracting(ReportGroupPagingBenchmark.PageMeasurement::totalElements)
                .containsOnly(25L);
        assertThat(measurements).extracting(ReportGroupPagingBenchmark.PageMeasurement::contentSize)
                .containsExactly(10, 10, 5);
    }

    private Report report(Member reporter, Long targetId) {
        return Report.builder()
                .reporter(reporter)
                .targetType(TargetType.POST)
                .targetId(targetId)
                .reasonType("SPAM")
                .reasonDetail("benchmark")
                .build();
    }

    @Test
    @Disabled("Local benchmark only. Enable manually with -Dreport.benchmark.groups=500000 for million-row style data.")
    void measureLargeOffsetPages() {
        int groupCount = Integer.getInteger("report.benchmark.groups", 10_000);
        int reportsPerGroup = Integer.getInteger("report.benchmark.reportsPerGroup", 2);
        int pageSize = Integer.getInteger("report.benchmark.pageSize", 20);

        List<Member> reporters = createReporters(reportsPerGroup);
        seedReports(groupCount, reporters);

        List<ReportGroupPagingBenchmark.PageMeasurement> measurements =
                ReportGroupPagingBenchmark.measureOffsetPages(
                        reportRepository,
                        ReportStatus.PENDING,
                        LocalDateTime.now().minusDays(1),
                        LocalDateTime.now().plusDays(1),
                        pageSize
                );

        measurements.forEach(measurement -> System.out.printf(
                "%s page=%d size=%d content=%d totalElements=%d totalPages=%d elapsedMillis=%d%n",
                measurement.label(),
                measurement.pageNumber(),
                measurement.pageSize(),
                measurement.contentSize(),
                measurement.totalElements(),
                measurement.totalPages(),
                measurement.elapsedMillis()
        ));
    }

    private List<Member> createReporters(int reportsPerGroup) {
        return java.util.stream.LongStream.rangeClosed(1, reportsPerGroup)
                .mapToObj(i -> Member.createLocalMember(
                        "benchmark-reporter-" + i + "@test.com",
                        "password123!",
                        "benchmarkReporter" + i
                ))
                .map(memberRepository::save)
                .toList();
    }

    private void seedReports(int groupCount, List<Member> reporters) {
        int persisted = 0;

        for (long targetId = 1; targetId <= groupCount; targetId++) {
            for (Member reporter : reporters) {
                entityManager.persist(report(reporter, targetId));
                persisted++;

                if (persisted % 1_000 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        }

        entityManager.flush();
        entityManager.clear();
    }
}
