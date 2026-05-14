package com.back.devc.domain.interaction.postLike.dto;

import java.time.LocalDateTime;

public record LikedPostResponse(
        Long postId,
        String title,
        String authorNickname,
        long likeCount,
        long commentCount,
        long viewCount,
        LocalDateTime createdAt,
        boolean liked,
        boolean bookmarked
) {
}