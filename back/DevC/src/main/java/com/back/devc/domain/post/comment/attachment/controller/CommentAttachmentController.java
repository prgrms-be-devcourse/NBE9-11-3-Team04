package com.back.devc.domain.post.comment.attachment.controller;

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentDeleteResponse;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentUploadRequest;
import com.back.devc.domain.post.comment.attachment.service.CommentAttachmentService;
import com.back.devc.global.security.jwt.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId;

import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.CommentAttachmentSuccessCode;

import java.util.List;

@RestController
@RequestMapping("/api/comments/{commentId}/attachments")
@RequiredArgsConstructor
public class CommentAttachmentController {

    private final CommentAttachmentService commentAttachmentService;

    /**
     * 댓글 첨부파일 업로드
     *
     * 첨부파일 업로드/삭제는 로그인한 사용자만 가능하도록 제한
     * 따라서 현재 로그인 사용자를 SecurityContext 안의 JwtPrincipal 에서 확인한 뒤 처리
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponse<CommentAttachmentListResponse>> uploadCommentAttachments(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long commentId,
            @ModelAttribute CommentAttachmentUploadRequest request
    ) {
        getAuthenticatedUserId(principal);

        CommentAttachmentListResponse response = commentAttachmentService.uploadAttachments(commentId, request);
        CommentAttachmentSuccessCode successCode = CommentAttachmentSuccessCode.COMMENT_ATTACHMENT_201_UPLOAD;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<CommentAttachmentListResponse>> getCommentAttachments(
            @PathVariable Long commentId
    ) {
        CommentAttachmentListResponse response = commentAttachmentService.getAttachments(commentId);
        CommentAttachmentSuccessCode successCode = CommentAttachmentSuccessCode.COMMENT_ATTACHMENT_200_LIST;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    /**
     * 댓글 첨부파일 삭제
     *
     * 업로드와 동일하게 현재 로그인한 사용자 기준으로만 요청을 허용
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<SuccessResponse<CommentAttachmentDeleteResponse>> deleteCommentAttachment(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long commentId,
            @PathVariable Long attachmentId
    ) {
        getAuthenticatedUserId(principal);

        CommentAttachmentDeleteResponse response = commentAttachmentService.deleteAttachment(commentId, attachmentId);
        CommentAttachmentSuccessCode successCode = CommentAttachmentSuccessCode.COMMENT_ATTACHMENT_200_DELETE;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

}