package com.back.devc.domain.post.comment.attachment.repository;

import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentAttachmentRepository extends JpaRepository<CommentAttachment, Long> {

    List<CommentAttachment> findByCommentIdOrderByFileOrderAscIdAsc(Long commentId);

    Optional<CommentAttachment> findByIdAndCommentId(Long attachmentId, Long commentId);
}