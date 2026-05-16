package com.back.devc.domain.post.comment.attachment.controller

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentDeleteResponse
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentUploadRequest
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.CommentAttachmentSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/comments/{commentId}/attachments")
class CommentAttachmentController(
    private val commentAttachmentService: CommentAttachmentService,
) {

    /**
     * 댓글 첨부파일 업로드
     *
     * 첨부파일 업로드/삭제는 로그인한 사용자만 가능하도록 제한
     * 따라서 현재 로그인 사용자를 SecurityContext 안의 JwtPrincipal 에서 확인한 뒤 처리
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadCommentAttachments(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable commentId: Long,
        @ModelAttribute request: CommentAttachmentUploadRequest,
    ): ResponseEntity<SuccessResponse<CommentAttachmentListResponse>> {
        getAuthenticatedUserId(principal)

        val response = commentAttachmentService.uploadAttachments(commentId, request)
        val successCode = CommentAttachmentSuccessCode.COMMENT_ATTACHMENT_201_UPLOAD

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping
    fun getCommentAttachments(
        @PathVariable commentId: Long,
    ): ResponseEntity<SuccessResponse<CommentAttachmentListResponse>> {
        val response = commentAttachmentService.getAttachments(commentId)
        val successCode = CommentAttachmentSuccessCode.COMMENT_ATTACHMENT_200_LIST

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 댓글 첨부파일 삭제
     *
     * 업로드와 동일하게 현재 로그인한 사용자 기준으로만 요청을 허용
     */
    @DeleteMapping("/{attachmentId}")
    fun deleteCommentAttachment(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable commentId: Long,
        @PathVariable attachmentId: Long,
    ): ResponseEntity<SuccessResponse<CommentAttachmentDeleteResponse>> {
        getAuthenticatedUserId(principal)

        val response = commentAttachmentService.deleteAttachment(commentId, attachmentId)
        val successCode = CommentAttachmentSuccessCode.COMMENT_ATTACHMENT_200_DELETE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }
}