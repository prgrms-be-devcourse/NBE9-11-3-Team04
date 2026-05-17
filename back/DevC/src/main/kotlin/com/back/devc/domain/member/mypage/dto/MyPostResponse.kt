package com.back.devc.domain.member.mypage.dto

import java.time.LocalDateTime

data class MyPostResponse(
    val postId: Long,
    val title: String,
    val likeCount: Long,
    val commentCount: Long,
    val viewCount: Long,
    val createdAt: LocalDateTime,
    val liked: Boolean,
    val bookmarked: Boolean,
)