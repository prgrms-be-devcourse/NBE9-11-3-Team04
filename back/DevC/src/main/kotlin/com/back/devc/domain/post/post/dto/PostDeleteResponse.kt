package com.back.devc.domain.post.post.dto

data class PostDeleteResponse(
    val postId: Long
) {
    companion object {
        @JvmStatic
        fun of(postId: Long): PostDeleteResponse {
            return PostDeleteResponse(postId)
        }
    }
}