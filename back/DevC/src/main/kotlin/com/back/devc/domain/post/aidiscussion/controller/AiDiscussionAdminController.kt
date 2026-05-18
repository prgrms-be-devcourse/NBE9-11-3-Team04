package com.back.devc.domain.post.aidiscussion.controller

import com.back.devc.domain.post.aidiscussion.dto.AiDiscussionPostResponse
import com.back.devc.domain.post.aidiscussion.service.AiDiscussionPostService
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import com.back.devc.global.security.jwt.JwtPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/ai-discussions")
class AiDiscussionAdminController(
    private val aiDiscussionPostService: AiDiscussionPostService,
) {

    @PostMapping("/pending")
    fun createPendingDiscussion(): AiDiscussionPostResponse {
        return aiDiscussionPostService.createPendingDiscussion()
    }

    @GetMapping("/pending")
    fun getPendingDiscussions(): List<AiDiscussionPostResponse> {
        return aiDiscussionPostService.getPendingDiscussions()
    }

    @GetMapping
    fun getDiscussions(
        @RequestParam(defaultValue = "PENDING") status: AiDiscussionStatus,
        pageable: Pageable,
    ): Page<AiDiscussionPostResponse> {
        return aiDiscussionPostService.getDiscussions(status, pageable)
    }

    @GetMapping("/{aiDiscussionPostId}")
    fun getDiscussion(
        @PathVariable aiDiscussionPostId: Long,
    ): AiDiscussionPostResponse {
        return aiDiscussionPostService.getDiscussion(aiDiscussionPostId)
    }

    @PatchMapping("/{aiDiscussionPostId}/approve")
    fun approveDiscussion(
        @PathVariable aiDiscussionPostId: Long,
        @AuthenticationPrincipal principal: JwtPrincipal,
        @RequestParam categoryId: Long,
    ): AiDiscussionPostResponse {
        return aiDiscussionPostService.approveDiscussion(
            aiDiscussionPostId = aiDiscussionPostId,
            adminUserId = principal.userId,
            categoryId = categoryId,
        )
    }

    @PatchMapping("/{aiDiscussionPostId}/reject")
    fun rejectDiscussion(
        @PathVariable aiDiscussionPostId: Long,
        @RequestParam(required = false) reason: String?,
    ): AiDiscussionPostResponse {
        return aiDiscussionPostService.rejectDiscussion(
            aiDiscussionPostId = aiDiscussionPostId,
            reason = reason,
        )
    }
}