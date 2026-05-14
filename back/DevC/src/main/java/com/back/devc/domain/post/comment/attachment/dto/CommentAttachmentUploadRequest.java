package com.back.devc.domain.post.comment.attachment.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CommentAttachmentUploadRequest(
        List<MultipartFile> files,
        List<Integer> fileOrders
) {
}
