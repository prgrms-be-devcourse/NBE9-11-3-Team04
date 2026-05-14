package com.back.devc.domain.interaction.report.dto;

import com.back.devc.domain.interaction.report.entity.SanctionType;
import com.back.devc.domain.interaction.report.entity.TargetType;

public record AdminReportRequestDTO(
        Long reportId,
        TargetType targetType,
        String adminNote,
        SanctionType sanctionType,
        Integer suspensionDays
) {
}