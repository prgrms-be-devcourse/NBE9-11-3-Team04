package com.back.devc.domain.interaction.bookmark.dto

data class BookmarkCreateCommand(
    val memberId: Long,
    val postId: Long,
)