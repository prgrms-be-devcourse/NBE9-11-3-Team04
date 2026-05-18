package com.back.devc.domain.post.aidiscussion.service

import com.back.devc.domain.post.aidiscussion.entity.AiDiscussionPost
import com.back.devc.domain.post.aidiscussion.repository.AiDiscussionPostRepository
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import com.back.devc.domain.post.post.dto.PostCreateRequest
import com.back.devc.domain.post.post.service.PostService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import com.back.devc.domain.post.aidiscussion.dto.AiDiscussionPostResponse


@Service
class AiDiscussionPostService(
    private val aiDiscussionPostRepository: AiDiscussionPostRepository,
    private val postService: PostService,
) {

    @Transactional
    fun createPendingDiscussion(): AiDiscussionPostResponse {
        if (aiDiscussionPostRepository.existsByStatus(AiDiscussionStatus.PENDING)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 승인 대기 중인 AI 토론 주제가 있습니다.",
            )
        }

        val aiDiscussionPost = AiDiscussionPost.create(
            title = "AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?",
            content = """
                최근 AI 도구가 코드 작성, 리팩토링, 테스트 코드 작성까지 도와주는 환경이 빠르게 확산되고 있습니다.

                이런 상황에서 주니어 개발자는 단순 구현 능력보다 문제 정의 능력, 코드 리뷰 능력, 설계 이해력이 더 중요해질 수 있다는 의견이 있습니다.

                여러분은 AI 시대에 주니어 개발자가 가장 집중해야 할 역량이 무엇이라고 생각하시나요?

                토론 포인트
                1. AI 도구를 잘 쓰는 능력도 개발 역량이라고 볼 수 있을까?
                2. 기초 CS 지식의 중요성은 줄어들까, 더 커질까?
                3. 기업은 앞으로 주니어 개발자에게 무엇을 기대하게 될까?
            """.trimIndent(),
        )

        val savedAiDiscussionPost = aiDiscussionPostRepository.save(aiDiscussionPost)

        return AiDiscussionPostResponse.from(savedAiDiscussionPost)
    }

    @Transactional(readOnly = true)
    fun getPendingDiscussions(): List<AiDiscussionPostResponse> {
        return aiDiscussionPostRepository.findAllByStatusOrderByCreatedAtDesc(
            AiDiscussionStatus.PENDING,
        ).map { aiDiscussionPost -> AiDiscussionPostResponse.from(aiDiscussionPost) }
    }

    @Transactional(readOnly = true)
    fun getDiscussions(status: AiDiscussionStatus): List<AiDiscussionPostResponse> {
        return aiDiscussionPostRepository.findAllByStatusOrderByCreatedAtDesc(status)
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