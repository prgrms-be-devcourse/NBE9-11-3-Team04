package com.back.devc.domain.post.comment.attachment.dto;

public record CommentAttachmentDeleteResponse(
        Long attachmentId,
        String message
) {
}