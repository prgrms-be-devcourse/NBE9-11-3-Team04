package com.back.devc.domain.interaction.postLike.dto;

/**
 * 내가 좋아요한 게시글 목록 조회 요청 DTO
 * Controller -> Service 전달용
 */
public record LikedPostsQuery(
        Long userId
) {
}