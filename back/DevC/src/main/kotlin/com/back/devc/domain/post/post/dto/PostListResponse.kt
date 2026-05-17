package com.back.devc.domain.post.post.dto

import com.back.devc.domain.member.member.util.MemberDisplayUtil
import com.back.devc.domain.post.post.entity.Post
import java.time.LocalDateTime

data class PostListResponse(
    val postId: Long,
    val title: String,
    val content: String,
    val userId: Long?,
    val nickName: String,
    val categoryId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val liked: Boolean,
    val bookmarked: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        @JvmStatic
        fun from(post: Post, liked: Boolean, bookmarked: Boolean): PostListResponse {
            return PostListResponse(
                postId = post.postId,
                title = post.title,
                content = post.content,
                userId = post.member?.userId,
                nickName = MemberDisplayUtil.getDisplayName(post.member),
                categoryId = post.category.categoryId,
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                liked = liked,
                bookmarked = bookmarked,
                createdAt = post.createdAt
            )
        }
    }
}