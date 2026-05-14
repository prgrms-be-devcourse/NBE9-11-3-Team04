package com.back.devc.domain.post.comment.dto;

public record CommentDeleteResponse(
        Long commentId,
        String message
) {
}