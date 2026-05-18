package com.back.devc.domain.post.post.entity

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.post.category.entity.Category
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@Entity
class Post protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    var postId: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var member: Member
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    lateinit var category: Category
        protected set

    @Column(name = "title", nullable = false, length = 100)
    lateinit var title: String
        protected set

    @Lob
    @Column(name = "content", nullable = false)
    lateinit var content: String
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Int = 0
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0
        protected set

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    private constructor(
        member: Member,
        category: Category,
        title: String,
        content: String,
    ) : this() {
        this.member = member
        this.category = category
        this.title = title
        this.content = content
    }

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun increaseViewCount() {
        viewCount++
    }

    fun increaseLikeCount() {
        likeCount++
    }

    fun decreaseLikeCount() {
        if (likeCount > 0) likeCount--
    }

    fun increaseCommentCount() {
        commentCount++
    }

    fun decreaseCommentCount() {
        if (commentCount > 0) commentCount--
    }

    fun update(
        title: String,
        content: String,
        category: Category,
    ) {
        this.title = title
        this.content = content
        this.category = category
    }

    fun delete() {
        isDeleted = true
        deletedAt = LocalDateTime.now()
    }

    fun syncCommentCount(actualCount: Int) {
        commentCount = actualCount
    }

    companion object {
        @JvmStatic
        fun create(
            member: Member,
            category: Category,
            title: String,
            content: String,
        ): Post {
            return Post(
                member = member,
                category = category,
                title = title,
                content = content,
            )
        }
    }
}