package com.back.devc.domain.member.member.dto;

import java.time.LocalDateTime;

public record PublicProfilePostResponse(
        Long postId,
        String title,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
}
