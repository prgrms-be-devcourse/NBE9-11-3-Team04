package com.back.devc.domain.post.comment.attachment.dto

import java.time.LocalDateTime

data class CommentAttachmentResponse(
    val attachmentId: Long,
    val commentId: Long,
    val fileName: String,
    val storedName: String,
    val fileUrl: String,
    val fileType: String,
    val mimeType: String,
    val fileSize: Long,
    val fileOrder: Int,
    val createdAt: LocalDateTime,
)