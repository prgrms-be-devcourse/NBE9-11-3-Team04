package com.back.devc.domain.interaction.postLike.dto;

/**
 * 좋아요 추가/취소 요청 DTO
 * Controller -> Service 전달용
 */
public record PostLikeCommand(
        Long userId,
        Long postId
) {
}