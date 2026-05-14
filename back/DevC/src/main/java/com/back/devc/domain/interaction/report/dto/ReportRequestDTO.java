package com.back.devc.domain.interaction.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequestDTO(

        @NotNull(message = "신고 대상 ID는 필수입니다.")
        Long targetId,

        @NotBlank(message = "신고 유형은 필수입니다.")
        String reasonType,

        String reasonDetail
) {
}