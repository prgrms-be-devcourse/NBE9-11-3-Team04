package com.back.devc.domain.interaction.postLike.controller

import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand
import com.back.devc.domain.interaction.postLike.dto.PostLikeResponse
import com.back.devc.domain.interaction.postLike.service.PostLikeService
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.PostLikeSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class PostLikeController(
    private val postLikeService: PostLikeService,
) {

    @PostMapping("/posts/{postId}/likes")
    fun createLike(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable postId: Long,
    ): ResponseEntity<SuccessResponse<PostLikeResponse>> {
        val command = PostLikeCommand(
            userId = getAuthenticatedUserId(principal),
            postId = postId,
        )

        val response = postLikeService.createLike(command)
        val successCode = PostLikeSuccessCode.POST_LIKE_CREATED

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @DeleteMapping("/posts/{postId}/likes")
    fun cancelLike(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable postId: Long,
    ): ResponseEntity<SuccessResponse<PostLikeResponse>> {
        val command = PostLikeCommand(
            userId = getAuthenticatedUserId(principal),
            postId = postId,
        )

        val response = postLikeService.cancelLike(command)
        val successCode = PostLikeSuccessCode.POST_LIKE_CANCELED

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping("/users/me/likes")
    fun getLikedPosts(
        @AuthenticationPrincipal principal: JwtPrincipal,
    ): ResponseEntity<SuccessResponse<List<LikedPostResponse>>> {
        val query = LikedPostsQuery(
            userId = getAuthenticatedUserId(principal),
        )

        val response = postLikeService.getLikedPosts(query)
        val successCode = PostLikeSuccessCode.LIKED_POSTS_FETCHED

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }
}