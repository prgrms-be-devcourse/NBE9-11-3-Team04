package com.back.devc.domain.post.comment.attachment.entity;

import com.back.devc.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "comment_attachments")
public class CommentAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "stored_name", nullable = false, unique = true)
    private String storedName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_order")
    private Integer fileOrder;

    protected CommentAttachment() {
    }

    public CommentAttachment(
            Long commentId,
            String fileName,
            String storedName,
            String fileUrl,
            String fileType,
            String mimeType,
            Long fileSize,
            Integer fileOrder
    ) {
        this.commentId = commentId;
        this.fileName = fileName;
        this.storedName = storedName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.fileOrder = fileOrder;
    }

    public static CommentAttachment create(
            Long commentId,
            String fileName,
            String storedName,
            String fileUrl,
            String fileType,
            String mimeType,
            Long fileSize,
            Integer fileOrder
    ) {
        return new CommentAttachment(
                commentId,
                fileName,
                storedName,
                fileUrl,
                fileType,
                mimeType,
                fileSize,
                fileOrder
        );
    }

    public Long getId() {
        return id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Integer getFileOrder() {
        return fileOrder;
    }
}