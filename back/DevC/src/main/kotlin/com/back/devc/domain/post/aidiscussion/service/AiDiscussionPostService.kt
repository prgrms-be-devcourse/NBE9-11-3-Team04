package com.back.devc.domain.post.aidiscussion.service

import com.back.devc.domain.post.aidiscussion.dto.AiDiscussionPostResponse
import com.back.devc.domain.post.aidiscussion.entity.AiDiscussionPost
import com.back.devc.domain.post.aidiscussion.repository.AiDiscussionPostRepository
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import com.back.devc.domain.post.post.dto.PostCreateRequest
import com.back.devc.domain.post.post.service.PostService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AiDiscussionPostService(
    private val aiDiscussionPostRepository: AiDiscussionPostRepository,
    private val postService: PostService,
    private val aiDiscussionGeneratorService: AiDiscussionGeneratorService,
    private val aiDiscussionPersistenceService: AiDiscussionPersistenceService,
) {

    fun createPendingDiscussion(): AiDiscussionPostResponse {
        aiDiscussionPersistenceService.validatePendingDiscussionDoesNotExist()

        val generatedTopic = aiDiscussionGeneratorService.generateDailyTopic()

        return aiDiscussionPersistenceService.savePendingDiscussion(
            title = generatedTopic.title,
            content = generatedTopic.content,
        )
    }

    @Transactional(readOnly = true)
    fun getPendingDiscussions(): List<AiDiscussionPostResponse> {
        return aiDiscussionPostRepository.findAllByStatusOrderByCreatedAtDesc(
            AiDiscussionStatus.PENDING,
        ).map { aiDiscussionPost -> AiDiscussionPostResponse.from(aiDiscussionPost) }
    }

    @Transactional(readOnly = true)
    fun getDiscussions(
        status: AiDiscussionStatus,
        pageable: Pageable,
    ): Page<AiDiscussionPostResponse> {
        return aiDiscussionPostRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
            .map { aiDiscussionPost -> AiDiscussionPostResponse.from(aiDiscussionPost) }
    }

    @Transactional(readOnly = true)
    fun getDiscussion(aiDiscussionPostId: Long): AiDiscussionPostResponse {
        val aiDiscussionPost = aiDiscussionPostRepository.findById(aiDiscussionPostId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "AI 토론 주제를 찾을 수 없습니다.",
                )
            }

        return AiDiscussionPostResponse.from(aiDiscussionPost)
    }

    @Transactional
    fun approveDiscussion(
        aiDiscussionPostId: Long,
        adminUserId: Long,
        categoryId: Long,
    ): AiDiscussionPostResponse {
        val aiDiscussionPost = aiDiscussionPostRepository.findById(aiDiscussionPostId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "AI 토론 주제를 찾을 수 없습니다.",
                )
            }

        if (aiDiscussionPost.status != AiDiscussionStatus.PENDING) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "승인 대기 상태의 AI 토론 주제만 승인할 수 있습니다.",
            )
        }

        val postCreateResponse = postService.write(
            userId = adminUserId,
            request = PostCreateRequest(
                title = aiDiscussionPost.title,
                content = aiDiscussionPost.content,
                categoryId = categoryId,
            ),
        )

        aiDiscussionPost.approve(postCreateResponse.postId)

        return AiDiscussionPostResponse.from(aiDiscussionPost)
    }

    @Transactional
    fun rejectDiscussion(
        aiDiscussionPostId: Long,
        reason: String?,
    ): AiDiscussionPostResponse {
        val aiDiscussionPost = aiDiscussionPostRepository.findById(aiDiscussionPostId)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "AI 토론 주제를 찾을 수 없습니다.",
                )
            }

        if (aiDiscussionPost.status != AiDiscussionStatus.PENDING) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "승인 대기 상태의 AI 토론 주제만 거절할 수 있습니다.",
            )
        }

        aiDiscussionPost.reject(reason)

        return AiDiscussionPostResponse.from(aiDiscussionPost)
    }
}

