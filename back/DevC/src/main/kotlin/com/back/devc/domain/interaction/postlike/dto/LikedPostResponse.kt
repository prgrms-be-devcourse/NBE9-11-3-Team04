package com.back.devc.domain.interaction.postLike.dto

import java.time.LocalDateTime

data class LikedPostResponse(
    val postId: Long,
    val title: String,
    val authorNickname: String,
    val likeCount: Long,
    val commentCount: Long,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val liked: Boolean,
    val bookmarked: Boolean,
)