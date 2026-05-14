package com.back.devc.domain.post.post.dto;

public record PostDeleteResponse(
        Long postId
) {
    public static PostDeleteResponse of(Long postId) {
        return new PostDeleteResponse(postId);
    }
}