package com.back.devc.domain.post.comment.attachment.dto

import org.springframework.web.multipart.MultipartFile

data class CommentAttachmentUploadRequest(
    val files: List<MultipartFile>,
    val fileOrders: List<Int>,
)