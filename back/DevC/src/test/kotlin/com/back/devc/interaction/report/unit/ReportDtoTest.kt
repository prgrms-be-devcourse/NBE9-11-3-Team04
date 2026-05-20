package com.back.devc.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO
import com.back.devc.domain.interaction.report.entity.Report
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.member.member.entity.Member
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Report DTOs")
internal class ReportDtoTest {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    @DisplayName("ReportRequestDTO accepts valid request")
    fun reportRequestDto_acceptsValidRequest() {
        val dto = ReportRequestDTO(10L, "SPAM", "Repeated promotion")

        val violations = validator.validate(dto)

        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("ReportRequestDTO requires target id and reason type")
    fun reportRequestDto_requiresTargetIdAndReasonType() {
        val targetIdViolations = validator.validateValue(
            ReportRequestDTO::class.java,
            "targetId",
            null
        )
        val reasonTypeViolations = validator.validate(ReportRequestDTO(10L, " ", null))

        assertThat(targetIdViolations.map { it.propertyPath.toString() })
            .containsExactly("targetId")
        assertThat(reasonTypeViolations.map { it.propertyPath.toString() })
            .containsExactly("reasonType")
    }

    @Test
    @DisplayName("ReportResponseDTO maps report and target information")
    fun reportResponseDto_mapsReportAndTargetInfo() {
        val reporter = Member.createLocalMember("reporter@test.com", "password", "reporter")
        val report = Report.create(
            reporter = reporter,
            targetType = TargetType.POST,
            targetId = 10L,
            reasonType = "SPAM",
            reasonDetail = "Repeated promotion"
        )
        val createdAt = LocalDateTime.of(2026, 1, 1, 10, 0)

        ReflectionTestUtils.setField(report, "reportId", 1L)
        ReflectionTestUtils.setField(report, "createdAt", createdAt)

        val dto = ReportResponseDTO.of(report, "target-writer", "target-title", "target-content")

        assertThat(dto.reportId).isEqualTo(1L)
        assertThat(dto.reporterEmail).isEqualTo("reporter@test.com")
        assertThat(dto.reporterNickname).isEqualTo("reporter")
        assertThat(dto.targetType).isEqualTo(TargetType.POST)
        assertThat(dto.targetId).isEqualTo(10L)
        assertThat(dto.targetNickname).isEqualTo("target-writer")
        assertThat(dto.targetTitle).isEqualTo("target-title")
        assertThat(dto.targetContent).isEqualTo("target-content")
        assertThat(dto.reasonType).isEqualTo("SPAM")
        assertThat(dto.reasonDetail).isEqualTo("Repeated promotion")
        assertThat(dto.status).isEqualTo(ReportStatus.PENDING)
        assertThat(dto.createdAt).isEqualTo(createdAt)
    }
}
