package com.back.devc.domain.interaction.bookmark.dto

data class BookmarkResponse(
    val postId: Long,
    val bookmarked: Boolean,
)