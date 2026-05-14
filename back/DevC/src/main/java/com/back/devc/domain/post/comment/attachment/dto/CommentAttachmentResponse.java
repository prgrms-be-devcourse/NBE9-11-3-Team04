package com.back.devc.domain.post.comment.attachment.dto;

import java.time.LocalDateTime;

public record CommentAttachmentResponse(
        Long attachmentId,
        Long commentId,
        String fileName,
        String storedName,
        String fileUrl,
        String fileType,
        String mimeType,
        Long fileSize,
        Integer fileOrder,
        LocalDateTime createdAt
) {
}
