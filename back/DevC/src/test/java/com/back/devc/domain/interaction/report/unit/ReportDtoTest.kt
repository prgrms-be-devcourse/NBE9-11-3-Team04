package com.back.devc.domain.interaction.report.unit

import com.back.devc.domain.interaction.report.dto.ReportRequestDTO
import com.back.devc.domain.interaction.report.dto.ReportResponseDTO.Companion.of
import com.back.devc.domain.interaction.report.entity.Report.Companion.create
import com.back.devc.domain.interaction.report.entity.ReportStatus
import com.back.devc.domain.interaction.report.entity.TargetType
import com.back.devc.domain.member.member.entity.Member.Companion.createLocalMember
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions
import org.assertj.core.api.iterable.ThrowingExtractor
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

@DisplayName("Report DTOs")
internal class ReportDtoTest {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().getValidator()

    @Test
    @DisplayName("ReportRequestDTO accepts valid request")
    fun reportRequestDto_acceptsValidRequest() {
        val dto = ReportRequestDTO(10L, "SPAM", "Repeated promotion")

        val violations = validator.validate<ReportRequestDTO?>(dto)

        Assertions.assertThat<ConstraintViolation<ReportRequestDTO?>?>(violations).isEmpty()
    }

    @Test
    @DisplayName("ReportRequestDTO requires target id and reason type")
    fun reportRequestDto_requiresTargetIdAndReasonType() {
        val targetIdViolations =
            validator.validateValue<ReportRequestDTO?>(ReportRequestDTO::class.java, "targetId", null)
        val dto = ReportRequestDTO(10L, " ", null)

        val reasonTypeViolations = validator.validate<ReportRequestDTO?>(dto)

        Assertions.assertThat<ConstraintViolation<ReportRequestDTO?>?>(targetIdViolations)
            .extracting<String?, RuntimeException?>(ThrowingExtractor { violation: ConstraintViolation<ReportRequestDTO?>? ->
                violation!!.getPropertyPath().toString()
            })
            .containsExactly("targetId")
        Assertions.assertThat<ConstraintViolation<ReportRequestDTO?>?>(reasonTypeViolations)
            .extracting<String?, RuntimeException?>(ThrowingExtractor { violation: ConstraintViolation<ReportRequestDTO?>? ->
                violation!!.getPropertyPath().toString()
            })
            .containsExactly("reasonType")
    }

    @Test
    @DisplayName("ReportResponseDTO maps report and target information")
    fun reportResponseDto_mapsReportAndTargetInfo() {
        val reporter = createLocalMember("reporter@test.com", "password", "reporter")
        val report = create(
            reporter,
            TargetType.POST,
            10L,
            "SPAM",
            "Repeated promotion"
        )

        val createdAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        ReflectionTestUtils.setField(report, "reportId", 1L)
        ReflectionTestUtils.setField(report, "createdAt", createdAt)

        val dto = of(report, "target-writer", "target-title", "target-content")

        Assertions.assertThat(dto.reportId).isEqualTo(1L)
        Assertions.assertThat(dto.reporterEmail).isEqualTo("reporter@test.com")
        Assertions.assertThat(dto.reporterNickname).isEqualTo("reporter")
        Assertions.assertThat<TargetType>(dto.targetType).isEqualTo(TargetType.POST)
        Assertions.assertThat(dto.targetId).isEqualTo(10L)
        Assertions.assertThat(dto.targetNickname).isEqualTo("target-writer")
        Assertions.assertThat(dto.targetTitle).isEqualTo("target-title")
        Assertions.assertThat(dto.targetContent).isEqualTo("target-content")
        Assertions.assertThat(dto.reasonType).isEqualTo("SPAM")
        Assertions.assertThat(dto.reasonDetail).isEqualTo("Repeated promotion")
        Assertions.assertThat<ReportStatus>(dto.status).isEqualTo(ReportStatus.PENDING)
        Assertions.assertThat(dto.createdAt).isEqualTo(createdAt)
    }
}
