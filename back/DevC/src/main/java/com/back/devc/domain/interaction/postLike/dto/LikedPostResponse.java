package com.back.devc.domain.interaction.postLike.dto;

import java.time.LocalDateTime;

/**
 * 사용자가 좋아요한 게시글 목록 응답 DTO
 */
public record LikedPostResponse(
        long postId,
        String title,
        String authorNickname,
        long likeCount,
        long commentCount,
        long viewCount,
        LocalDateTime createdAt
) {
}