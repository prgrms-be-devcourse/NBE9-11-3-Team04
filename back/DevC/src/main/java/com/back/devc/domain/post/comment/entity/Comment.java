package com.back.devc.domain.post.comment.entity;

import com.back.devc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Comment() {
    }

    public Comment(Long postId, Long userId, Long parentCommentId, String content) {
        this.postId = postId;
        this.userId = userId;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.isDeleted = false;
    }

    public static Comment create(Long postId, Long userId, Long parentCommentId, String content) {
        return new Comment(postId, userId, parentCommentId, content);
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.content = "삭제된 댓글입니다.";
    }

    public boolean isOwner(Long loginUserId) {
        return this.userId.equals(loginUserId);
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public String getContent() {
        return content;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}