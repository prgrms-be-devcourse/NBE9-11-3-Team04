package com.back.devc.domain.member.searchLog.dto;

public record PopularKeywordResponse(
        String keyword,
        long count
) {
}
