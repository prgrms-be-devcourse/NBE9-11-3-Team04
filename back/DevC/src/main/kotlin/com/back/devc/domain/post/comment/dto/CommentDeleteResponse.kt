package com.back.devc.domain.post.comment.dto

data class CommentDeleteResponse(
    val commentId: Long,
    val message: String,
)