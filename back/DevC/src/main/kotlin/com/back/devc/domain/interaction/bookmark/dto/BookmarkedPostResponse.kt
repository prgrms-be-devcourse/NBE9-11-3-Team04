package com.back.devc.domain.interaction.bookmark.dto

import java.time.LocalDateTime

data class BookmarkedPostResponse(
    val postId: Long,
    val title: String,
    val authorNickname: String,
    val categoryId: Long,
    val likeCount: Long,
    val commentCount: Long,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val liked: Boolean,
    val bookmarked: Boolean,
)