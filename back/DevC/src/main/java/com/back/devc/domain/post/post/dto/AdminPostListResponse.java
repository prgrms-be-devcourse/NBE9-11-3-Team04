package com.back.devc.domain.post.post.dto;

import com.back.devc.domain.post.post.entity.Post;

import java.time.LocalDateTime;

//게시글 전체 조회로 간략한 정보만 담아서 전달
public record AdminPostListResponse(
        Long postId,
        String title,
        Long userId,
        Long categoryId,
        Boolean isDeleted,
        int viewCount,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
    public AdminPostListResponse(Post post) {
        this(
                post.getPostId(),
                post.getTitle(),
                post.getMember() != null ? post.getMember().getUserId() : null,
                post.getCategory().getCategoryId(),
                post.isDeleted(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt()
        );
    }
}