package com.back.devc.domain.post.comment.dto

data class CommentListResponse(
    val comments: List<CommentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    constructor(comments: List<CommentResponse>) : this(
        comments = comments,
        page = 0,
        size = comments.size,
        totalElements = comments.size.toLong(),
        totalPages = 1,
        hasNext = false,
    )
}