package com.back.devc.domain.post.post.dto

import com.back.devc.domain.member.member.util.MemberDisplayUtil
import com.back.devc.domain.post.post.entity.Post
import java.time.LocalDateTime

data class PostDetailResponse(
    val postId: Long,
    val title: String,
    val content: String,
    val userId: Long?,
    val writerName: String,
    val categoryId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val bookmarkCount: Int,
    val bookmarked: Boolean,
    val liked: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        @JvmStatic
        fun from(
            post: Post,
            liked: Boolean = false,
            bookmarked: Boolean = false,
            bookmarkCount: Int = 0
        ): PostDetailResponse {
            return PostDetailResponse(
                postId = post.postId
                    ?: throw IllegalStateException("Post ID cannot be null"),
                title = post.title,
                content = post.content,
                userId = post.member?.userId,
                writerName = MemberDisplayUtil.getDisplayName(post.member),
                categoryId = post.category.categoryId,
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                bookmarkCount = bookmarkCount,
                bookmarked = bookmarked,
                liked = liked,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}