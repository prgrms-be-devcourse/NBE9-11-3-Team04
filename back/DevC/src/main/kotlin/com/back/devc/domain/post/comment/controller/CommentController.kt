package com.back.devc.domain.post.comment.controller

import com.back.devc.domain.post.comment.dto.CommentCreateRequest
import com.back.devc.domain.post.comment.dto.CommentDeleteResponse
import com.back.devc.domain.post.comment.dto.CommentListResponse
import com.back.devc.domain.post.comment.dto.CommentResponse
import com.back.devc.domain.post.comment.dto.CommentUpdateRequest
import com.back.devc.domain.post.comment.service.CommentService
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.CommentSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class CommentController(
    private val commentService: CommentService,
) {

    /**
     * 댓글 작성
     *
     * 댓글 작성자는 프론트에서 직접 넘기지 않고,
     * 현재 로그인한 사용자의 정보를 SecurityContext 안의 JwtPrincipal에서 꺼내 사용한다.
     * 이렇게 해야 다른 사용자의 userId를 임의로 넣어 댓글을 작성하는 것을 막을 수 있다.
     */
    @PostMapping("/posts/{postId}/comments")
    fun createComment(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable postId: Long,
        @Valid @RequestBody request: CommentCreateRequest,
    ): ResponseEntity<SuccessResponse<CommentResponse>> {
        val response = commentService.createComment(
            postId,
            getAuthenticatedUserId(principal),
            request,
        )
        val successCode = CommentSuccessCode.COMMENT_201_CREATE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 대댓글(답글) 작성
     *
     * 답글도 일반 댓글과 동일하게 현재 로그인한 사용자 기준으로만 작성
     */
    @PostMapping("/comments/{commentId}/replies")
    fun createReply(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentCreateRequest,
    ): ResponseEntity<SuccessResponse<CommentResponse>> {
        val response = commentService.createReply(
            commentId,
            getAuthenticatedUserId(principal),
            request,
        )
        val successCode = CommentSuccessCode.COMMENT_201_REPLY

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 댓글 수정
     *
     * 수정 요청 역시 현재 로그인한 사용자 기준으로 처리해서,
     * service 계층에서 "본인이 작성한 댓글인지"를 검증할 수 있게 함
     */
    @PatchMapping("/comments/{commentId}")
    fun updateComment(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentUpdateRequest,
    ): ResponseEntity<SuccessResponse<CommentResponse>> {
        val response = commentService.updateComment(
            commentId,
            getAuthenticatedUserId(principal),
            request,
        )
        val successCode = CommentSuccessCode.COMMENT_200_UPDATE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 댓글 삭제
     *
     * 삭제도 현재 로그인한 사용자 기준으로만 처리
     * 실제 삭제 가능 여부(본인 댓글인지 등)는 service 계층에서 검증
     */
    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable commentId: Long,
    ): ResponseEntity<SuccessResponse<CommentDeleteResponse>> {
        val response = commentService.deleteComment(
            commentId,
            getAuthenticatedUserId(principal),
        )
        val successCode = CommentSuccessCode.COMMENT_200_DELETE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping("/posts/{postId}/comments")
    fun getComments(
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<SuccessResponse<CommentListResponse>> {
        val response = commentService.getComments(postId, page, size)
        val successCode = CommentSuccessCode.COMMENT_200_LIST

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }
}