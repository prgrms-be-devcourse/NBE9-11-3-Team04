package com.back.devc.domain.interaction.report.dto;

import com.back.devc.domain.interaction.report.entity.Report;
import com.back.devc.domain.interaction.report.entity.ReportStatus;
import com.back.devc.domain.interaction.report.entity.TargetType;
import com.back.devc.domain.member.member.entity.Member;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Report DTOs")
class ReportDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("ReportRequestDTO accepts valid request")
    void reportRequestDto_acceptsValidRequest() {
        ReportRequestDTO dto = new ReportRequestDTO(10L, "SPAM", "Repeated promotion");

        var violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ReportRequestDTO requires target id and reason type")
    void reportRequestDto_requiresTargetIdAndReasonType() {
        var targetIdViolations = validator.validateValue(ReportRequestDTO.class, "targetId", null);
        ReportRequestDTO dto = new ReportRequestDTO(10L, " ", null);

        var reasonTypeViolations = validator.validate(dto);

        assertThat(targetIdViolations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("targetId");
        assertThat(reasonTypeViolations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("reasonType");
    }

    @Test
    @DisplayName("ReportResponseDTO maps report and target information")
    void reportResponseDto_mapsReportAndTargetInfo() {
        Member reporter = Member.createLocalMember("reporter@test.com", "password", "reporter");
        Report report = Report.builder()
                .reporter(reporter)
                .targetType(TargetType.POST)
                .targetId(10L)
                .reasonType("SPAM")
                .reasonDetail("Repeated promotion")
                .build();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        ReflectionTestUtils.setField(report, "reportId", 1L);
        ReflectionTestUtils.setField(report, "createdAt", createdAt);

        ReportResponseDTO dto = ReportResponseDTO.of(report, "target-writer", "target-title", "target-content");

        assertThat(dto.reportId).isEqualTo(1L);
        assertThat(dto.reporterEmail).isEqualTo("reporter@test.com");
        assertThat(dto.reporterNickname).isEqualTo("reporter");
        assertThat(dto.targetType).isEqualTo(TargetType.POST);
        assertThat(dto.targetId).isEqualTo(10L);
        assertThat(dto.targetNickname).isEqualTo("target-writer");
        assertThat(dto.targetTitle).isEqualTo("target-title");
        assertThat(dto.targetContent).isEqualTo("target-content");
        assertThat(dto.reasonType).isEqualTo("SPAM");
        assertThat(dto.reasonDetail).isEqualTo("Repeated promotion");
        assertThat(dto.status).isEqualTo(ReportStatus.PENDING);
        assertThat(dto.createdAt).isEqualTo(createdAt);
    }
}
