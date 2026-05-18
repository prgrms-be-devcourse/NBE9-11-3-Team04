package com.back.devc.domain.post.aidiscussion.entity

import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import com.back.devc.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "ai_discussion_posts")
class AiDiscussionPost protected constructor(
    title: String,
    content: String,
    status: AiDiscussionStatus = AiDiscussionStatus.PENDING,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_discussion_post_id")
    var id: Long? = null
        protected set

    @Column(nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String = content
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AiDiscussionStatus = status
        protected set

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    var rejectionReason: String? = null
        protected set

    @Column(name = "approved_post_id")
    var approvedPostId: Long? = null
        protected set

    fun approve(approvedPostId: Long) {
        this.status = AiDiscussionStatus.APPROVED
        this.approvedPostId = approvedPostId
        this.rejectionReason = null
    }

    fun reject(reason: String?) {
        this.status = AiDiscussionStatus.REJECTED
        this.rejectionReason = reason
    }

    companion object {
        fun create(
            title: String,
            content: String,
        ): AiDiscussionPost {
            return AiDiscussionPost(
                title = title,
                content = content,
                status = AiDiscussionStatus.PENDING,
            )
        }
    }
}