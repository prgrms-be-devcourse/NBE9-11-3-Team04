package com.back.devc.domain.post.post.dto

import com.back.devc.domain.post.post.entity.Post

data class PostCreateResponse(
    val postId: Long
) {
    companion object {
        @JvmStatic
        fun from(post: Post): PostCreateResponse {
            return PostCreateResponse(
                postId = post.postId
                    ?: throw IllegalStateException("Post ID cannot be null"),
            )
        }
    }
}