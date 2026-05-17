package com.back.devc.domain.post.post.dto

import com.back.devc.domain.post.post.entity.Post

data class PostUpdateResponse(
    val postId: Long,
    val title: String,
    val content: String,
    val categoryId: Long
) {
    companion object {
        @JvmStatic
        fun from(post: Post): PostUpdateResponse {
            return PostUpdateResponse(
                postId = post.postId,
                title = post.title,
                content = post.content,
                categoryId = post.category.categoryId
            )
        }
    }
}