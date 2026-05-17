package com.back.devc.domain.post.post.dto

import com.back.devc.domain.post.post.entity.Post
import java.time.LocalDateTime

// 게시글 전체 조회용 간략 정보 DTO
data class AdminPostListResponse(
    val postId: Long,
    val title: String,
    val userId: Long?,
    val categoryId: Long,
    val isDeleted: Boolean,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): AdminPostListResponse {
            return AdminPostListResponse(
                postId = post.postId,
                title = post.title,
                userId = post.member?.userId,
                categoryId = post.category.categoryId,
                isDeleted = post.isDeleted,
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                createdAt = post.createdAt
            )
        }
    }
}