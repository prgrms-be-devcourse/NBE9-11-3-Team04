package com.back.devc.domain.interaction.bookmark.dto;

import java.time.LocalDateTime;

public record BookmarkedPostResponse(
        long postId,
        String title,
        String authorNickname,
        long categoryId,
        long likeCount,
        long commentCount,
        long viewCount,
        LocalDateTime createdAt
) {
}