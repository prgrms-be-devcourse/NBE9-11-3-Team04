package com.back.devc.domain.interaction.report.dto;

import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.TargetType;

import java.time.LocalDateTime;

public record ReportResponseDTO(

        Long reportId,

        String reporterEmail,
        String reporterNickname,

        TargetType targetType,
        Long targetId,

        String targetNickname,
        String targetTitle,
        String targetContent,

        String reasonType,
        String reasonDetail,

        ReportStatus status,

        LocalDateTime createdAt,
        LocalDateTime processedAt
) {

    public static ReportResponseDTO of(
            Report report,
            String targetNickname,
            String targetTitle,
            String targetContent
    ) {
        return new ReportResponseDTO(
                report.getReportId(),
                report.getReporter().getEmail(),
                report.getReporter().getNickname(),
                report.getTargetType(),
                report.getTargetId(),
                targetNickname,
                targetTitle,
                targetContent,
                report.getReasonType(),
                report.getReasonDetail(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getProcessedAt()
        );
    }
}