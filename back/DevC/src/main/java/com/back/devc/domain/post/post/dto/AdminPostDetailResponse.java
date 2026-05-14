package com.back.devc.domain.post.post.dto;

import com.back.devc.domain.post.post.entity.Post;

import java.time.LocalDateTime;

public record AdminPostDetailResponse(
        Long postId,
        String title,
        String content,
        Long userId,
        String writerName,
        Long categoryId,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deleteAt
) {
    public static AdminPostDetailResponse from(Post post) {
        return new AdminPostDetailResponse(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                post.getMember() != null ? post.getMember().getUserId() : null,
                post.getMember() != null ? post.getMember().getNickname() : null,
                post.getCategory().getCategoryId(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.isDeleted(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getDeletedAt()
        );
    }
}