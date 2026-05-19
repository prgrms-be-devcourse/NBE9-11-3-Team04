package com.back.devc.domain.post.aidiscussion.service

import com.back.devc.domain.post.aidiscussion.dto.AiDiscussionPostResponse
import com.back.devc.domain.post.aidiscussion.entity.AiDiscussionPost
import com.back.devc.domain.post.aidiscussion.repository.AiDiscussionPostRepository
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AiDiscussionPersistenceService(
    private val aiDiscussionPostRepository: AiDiscussionPostRepository,
) {

    @Transactional(readOnly = true)
    fun validatePendingDiscussionDoesNotExist() {
        if (aiDiscussionPostRepository.existsByStatus(AiDiscussionStatus.PENDING)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 승인 대기 중인 AI 토론 주제가 있습니다.",
            )
        }
    }

    @Transactional
    fun savePendingDiscussion(
        title: String,
        content: String,
    ): AiDiscussionPostResponse {
        if (aiDiscussionPostRepository.existsByStatus(AiDiscussionStatus.PENDING)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 승인 대기 중인 AI 토론 주제가 있습니다.",
            )
        }

        val aiDiscussionPost = AiDiscussionPost.create(
            title = title,
            content = content,
        )

        val savedAiDiscussionPost = aiDiscussionPostRepository.save(aiDiscussionPost)

        return AiDiscussionPostResponse.from(savedAiDiscussionPost)
    }
}