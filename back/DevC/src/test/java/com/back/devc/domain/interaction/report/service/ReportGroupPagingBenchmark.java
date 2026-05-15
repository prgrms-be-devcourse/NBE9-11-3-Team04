package com.back.devc.domain.interaction.report.service;

import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.repository.ReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        PageMeasurement first = measure(reportRepository, status, from, to, 0, pageSize, "first");

        int lastPageNumber = Math.max(0, first.totalPages() - 1);
        int middlePageNumber = lastPageNumber / 2;

        Map<Integer, PageMeasurement> measurements = new LinkedHashMap<>();
        measurements.put(0, first);
        measurements.put(middlePageNumber, measure(
                reportRepository,
                status,
                from,
                to,
                middlePageNumber,
                pageSize,
                "middle"
        ));
        measurements.put(lastPageNumber, measure(
                reportRepository,
                status,
                from,
                to,
                lastPageNumber,
                pageSize,
                "last"
        ));

        return new ArrayList<>(measurements.values());
    }

    private static PageMeasurement measure(
            ReportRepository reportRepository,
            ReportStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int pageNumber,
            int pageSize,
            String label
    ) {
        long startedAt = System.nanoTime();
        Page<Object[]> page = reportRepository.findGroupedReports(
                status,
                from,
                to,
                PageRequest.of(pageNumber, pageSize)
        );
        long elapsedNanos = System.nanoTime() - startedAt;

        return new PageMeasurement(
                label,
                pageNumber,
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.getTotalElements(),
                page.getTotalPages(),
                elapsedNanos
        );
    }

    record PageMeasurement(
            String label,
            int requestedPageNumber,
            int pageNumber,
            int pageSize,
            int contentSize,
            long totalElements,
            int totalPages,
            long elapsedNanos
    ) {

        long elapsedMillis() {
            return elapsedNanos / 1_000_000;
        }
    }
}
