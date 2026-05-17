package com.back.devc.domain.member.mypage.dto

import java.time.LocalDateTime

data class MyCommentResponse(
    val commentId: Long,
    val postId: Long,
    val postTitle: String,
    val content: String,
    val createdAt: LocalDateTime,
)