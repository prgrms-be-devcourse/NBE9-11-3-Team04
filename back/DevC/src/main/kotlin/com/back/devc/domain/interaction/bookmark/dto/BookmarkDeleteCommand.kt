package com.back.devc.domain.interaction.bookmark.dto

data class BookmarkDeleteCommand(
    val memberId: Long,
    val postId: Long,
)