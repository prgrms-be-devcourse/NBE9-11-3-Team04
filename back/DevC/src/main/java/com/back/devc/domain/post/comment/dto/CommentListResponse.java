package com.back.devc.domain.post.comment.dto;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> comments,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public CommentListResponse(List<CommentResponse> comments) {
        this(comments, 0, comments.size(), comments.size(), 1, false);
    }
}