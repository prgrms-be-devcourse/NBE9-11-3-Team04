package com.back.devc.domain.interaction.bookmark.dto;

import java.time.LocalDateTime;

public record BookmarkedPostResponse(
        Long postId,
        String title,
        String authorNickname,
        Long categoryId,
        long likeCount,
        long commentCount,
        long viewCount,
        LocalDateTime createdAt,
        boolean liked,
        boolean bookmarked
) {
}