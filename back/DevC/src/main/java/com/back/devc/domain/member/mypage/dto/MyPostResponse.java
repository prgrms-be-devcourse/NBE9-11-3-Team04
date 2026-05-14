package com.back.devc.domain.member.mypage.dto;

import java.time.LocalDateTime;

public record MyPostResponse(
        Long postId,
        String title,
        long likeCount,
        long commentCount,
        long viewCount,
        LocalDateTime createdAt
) {
}