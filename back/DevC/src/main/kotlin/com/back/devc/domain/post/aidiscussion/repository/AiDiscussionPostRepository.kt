package com.back.devc.domain.post.aidiscussion.repository

import com.back.devc.domain.post.aidiscussion.entity.AiDiscussionPost
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import org.springframework.data.jpa.repository.JpaRepository

interface AiDiscussionPostRepository : JpaRepository<AiDiscussionPost, Long> {
    fun existsByStatus(status: AiDiscussionStatus): Boolean

    fun findAllByStatusOrderByCreatedAtDesc(
        status: AiDiscussionStatus,
    ): List<AiDiscussionPost>
}