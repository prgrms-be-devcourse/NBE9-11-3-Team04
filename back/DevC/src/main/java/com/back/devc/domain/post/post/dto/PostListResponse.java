package com.back.devc.domain.post.post.dto;

import com.back.devc.domain.member.member.util.MemberDisplayUtil;
import com.back.devc.domain.post.post.entity.Post;

import java.time.LocalDateTime;

import lombok.Builder;

public record PostListResponse(
        Long postId,
        String title,
        String content,
        Long userId,
        String nickName,
        Long categoryId,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean liked,
        boolean bookmarked,
        LocalDateTime createdAt
) {
    public static PostListResponse from(Post post, boolean liked, boolean bookmarked) {
        return new PostListResponse(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                post.getMember() != null ? post.getMember().getUserId() : null,
                MemberDisplayUtil.getDisplayName(post.getMember()),
                post.getCategory().getCategoryId(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                liked,
                bookmarked,
                post.getCreatedAt()
        );
    }
}