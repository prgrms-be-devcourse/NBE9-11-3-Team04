package com.back.devc.domain.post.comment.dto

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentResponse
import java.time.LocalDateTime

data class CommentResponse(
    val commentId: Long,
    val postId: Long,
    val postTitle: String,
    val userId: Long,
    val nickname: String,
    val parentCommentId: Long?,
    val content: String,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val replies: MutableList<CommentResponse> = mutableListOf(),
    val attachments: MutableList<CommentAttachmentResponse> = mutableListOf(),
) {
    companion object {
        @JvmStatic
        fun of(
            commentId: Long,
            postId: Long,
            postTitle: String,
            userId: Long,
            nickname: String,
            parentCommentId: Long?,
            content: String,
            isDeleted: Boolean,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): CommentResponse {
            return CommentResponse(
                commentId = commentId,
                postId = postId,
                postTitle = postTitle,
                userId = userId,
                nickname = nickname,
                parentCommentId = parentCommentId,
                content = content,
                isDeleted = isDeleted,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }
}