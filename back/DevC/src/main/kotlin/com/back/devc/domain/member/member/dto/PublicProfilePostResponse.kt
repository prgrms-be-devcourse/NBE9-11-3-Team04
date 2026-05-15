package com.back.devc.domain.member.member.dto

import java.time.LocalDateTime

data class PublicProfilePostResponse(
    val postId: Long,
    val title: String,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: LocalDateTime
)
