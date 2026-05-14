package com.back.devc.domain.member.searchLog.dto;

import java.time.LocalDateTime;

public record SearchLogResponse(
        Long searchLogId,
        String keyword,
        LocalDateTime searchedAt
) {
}
