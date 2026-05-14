package com.back.devc.domain.post.post.dto;

import com.back.devc.domain.post.post.entity.Post;

public record PostCreateResponse(
        Long postId
) {
    public static PostCreateResponse from(Post post) {
        return new PostCreateResponse(post.getPostId());
    }
}