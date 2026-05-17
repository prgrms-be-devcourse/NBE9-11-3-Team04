package com.back.devc.domain.interaction.postLike.dto

/**
 * 좋아요 추가/취소 응답 DTO
 */
data class PostLikeResponse(
    val postId: Long,
    val liked: Boolean,
    val likeCount: Long,
    val message: String,
)