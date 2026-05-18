package com.back.devc.domain.post.comment.attachment.repository

import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentAttachmentRepository : JpaRepository<CommentAttachment, Long> {
    fun findByCommentIdOrderByFileOrderAscIdAsc(commentId: Long): List<CommentAttachment>

    fun findByIdAndCommentId(
        attachmentId: Long,
        commentId: Long,
    ): CommentAttachment?
}