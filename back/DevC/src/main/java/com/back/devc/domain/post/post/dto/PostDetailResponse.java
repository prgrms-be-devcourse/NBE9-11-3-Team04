package com.back.devc.domain.post.post.dto;

import com.back.devc.domain.member.member.util.MemberDisplayUtil;
import com.back.devc.domain.post.post.entity.Post;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,
        String title,
        String content,
        Long userId,
        String writerName,
        Long categoryId,
        int viewCount,
        int likeCount,
        int commentCount,
        // 게시글 상세 화면에서 북마크 수를 바로 표시하기 위해 함께 내려주는 값
        int bookmarkCount,
        // 현재 로그인 사용자가 이 게시글을 북마크했는지 여부
        boolean bookmarked,
        boolean liked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PostDetailResponse from(Post post) {
        return from(post, false, false, 0);
    }

    public static PostDetailResponse from(Post post, boolean liked) {
        return from(post, liked, false, 0);
    }

    /**
     * 게시글 상세조회 응답 생성
     */
    public static PostDetailResponse from(Post post, boolean liked, boolean bookmarked, int bookmarkCount) {
        return new PostDetailResponse(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                post.getMember() != null ? post.getMember().getUserId() : null,
                MemberDisplayUtil.getDisplayName(post.getMember()),
                post.getCategory().getCategoryId(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                bookmarkCount,
                bookmarked,
                liked,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}