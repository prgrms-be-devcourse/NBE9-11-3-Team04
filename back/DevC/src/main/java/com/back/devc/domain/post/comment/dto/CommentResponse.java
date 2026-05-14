package com.back.devc.domain.post.comment.dto;

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record CommentResponse(
        Long commentId,
        Long postId,
        String postTitle,
        Long userId,
        String nickname,
        Long parentCommentId,
        String content,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> replies,
        List<CommentAttachmentResponse> attachments
) {

    public static CommentResponse of(
            Long commentId,
            Long postId,
            String postTitle,
            Long userId,
            String nickname,
            Long parentCommentId,
            String content,
            boolean isDeleted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new CommentResponse(
                commentId,
                postId,
                postTitle,
                userId,
                nickname,
                parentCommentId,
                content,
                isDeleted,
                createdAt,
                updatedAt,
                new ArrayList<>(),
                new ArrayList<>()
        );
    }
}