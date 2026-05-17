package com.back.devc.domain.post.comment.entity

import com.back.devc.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
class Comment protected constructor() : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    var id: Long? = null
        protected set

    @Column(name = "post_id", nullable = false)
    var postId: Long? = null
        protected set

    @Column(name = "user_id", nullable = false)
    private var userId: Long? = null

    @Column(name = "parent_comment_id")
    var parentCommentId: Long? = null
        protected set

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String? = null
        protected set

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    constructor(
        postId: Long,
        userId: Long,
        parentCommentId: Long?,
        content: String,
    ) : this() {
        this.postId = postId
        this.userId = userId
        this.parentCommentId = parentCommentId
        this.content = content
        this.isDeleted = false
    }

    fun updateContent(content: String) {
        this.content = content
    }

    fun softDelete() {
        this.isDeleted = true
        this.deletedAt = LocalDateTime.now()
        this.content = "삭제된 댓글입니다."
    }

    fun isOwner(loginUserId: Long?): Boolean {
        return this.userId == loginUserId
    }

    fun getUserId(): Long {
        return userId!!
    }

    companion object {
        @JvmStatic
        fun create(
            postId: Long,
            userId: Long,
            parentCommentId: Long?,
            content: String,
        ): Comment {
            return Comment(
                postId,
                userId,
                parentCommentId,
                content,
            )
        }
    }
}