package com.back.devc.domain.post.post.dto

import com.back.devc.domain.post.post.entity.Post
import java.time.LocalDateTime

data class AdminPostDetailResponse(
    val postId: Long,
    val title: String,
    val content: String,
    val userId: Long?,
    val writerName: String?,
    val categoryId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun from(post: Post): AdminPostDetailResponse {
            return AdminPostDetailResponse(
                postId = post.postId
                    ?: throw IllegalStateException("Post ID cannot be null"),
                title = post.title,
                content = post.content,
                userId = post.member?.userId,
                writerName = post.member?.nickname,
                categoryId = post.category.categoryId,
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                isDeleted = post.isDeleted,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
                deletedAt = post.deletedAt
            )
        }
    }
}