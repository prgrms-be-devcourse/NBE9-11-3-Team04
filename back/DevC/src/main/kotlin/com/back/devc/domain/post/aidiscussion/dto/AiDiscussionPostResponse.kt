package com.back.devc.domain.post.aidiscussion.dto

import com.back.devc.domain.post.aidiscussion.entity.AiDiscussionPost
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import java.time.LocalDateTime

data class AiDiscussionPostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val status: AiDiscussionStatus,
    val rejectionReason: String?,
    val approvedPostId: Long?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(aiDiscussionPost: AiDiscussionPost): AiDiscussionPostResponse {
            return AiDiscussionPostResponse(
                id = requireNotNull(aiDiscussionPost.id),
                title = aiDiscussionPost.title,
                content = aiDiscussionPost.content,
                status = aiDiscussionPost.status,
                rejectionReason = aiDiscussionPost.rejectionReason,
                approvedPostId = aiDiscussionPost.approvedPostId,
                createdAt = aiDiscussionPost.createdAt,
            )
        }
    }
}