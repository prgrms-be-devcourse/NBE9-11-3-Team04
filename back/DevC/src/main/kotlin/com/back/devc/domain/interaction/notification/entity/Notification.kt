package com.back.devc.domain.interaction.notification.entity

import com.back.devc.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notifications")
class Notification protected constructor() : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    var id: Long? = null
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long? = null
        protected set

    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: Long? = null
        protected set

    @Column(name = "post_id")
    var postId: Long? = null
        protected set

    @Column(name = "comment_id")
    var commentId: Long? = null
        protected set

    @Column(name = "type", nullable = false, length = 30)
    var type: String? = null
        protected set

    @Column(name = "message", nullable = false, length = 255)
    var message: String? = null
        protected set

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
        protected set

    constructor(
        userId: Long?,
        actorUserId: Long?,
        postId: Long?,
        commentId: Long?,
        type: String?,
        message: String?,
    ) : this() {
        this.userId = userId
        this.actorUserId = actorUserId
        this.postId = postId
        this.commentId = commentId
        this.type = type
        this.message = message
        this.isRead = false
    }

    fun markAsRead() {
        this.isRead = true
    }

    companion object {
        @JvmStatic
        fun create(
            userId: Long?,
            actorUserId: Long?,
            postId: Long?,
            commentId: Long?,
            type: String?,
            message: String?,
        ): Notification {
            return Notification(
                userId,
                actorUserId,
                postId,
                commentId,
                type,
                message,
            )
        }
    }
}