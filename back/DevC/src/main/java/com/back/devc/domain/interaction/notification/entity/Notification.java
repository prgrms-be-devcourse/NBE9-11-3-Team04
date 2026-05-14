package com.back.devc.domain.interaction.notification.entity;

import com.back.devc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    protected Notification() {
    }

    public Notification(Long userId, Long actorUserId, Long postId, Long commentId, String type, String message) {
        this.userId = userId;
        this.actorUserId = actorUserId;
        this.postId = postId;
        this.commentId = commentId;
        this.type = type;
        this.message = message;
        this.isRead = false;
    }

    public static Notification create(Long userId, Long actorUserId, Long postId, Long commentId, String type, String message) {
        return new Notification(userId, actorUserId, postId, commentId, type, message);
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return isRead;
    }
}