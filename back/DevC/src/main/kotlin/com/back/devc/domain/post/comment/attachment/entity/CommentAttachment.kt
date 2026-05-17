package com.back.devc.domain.post.comment.attachment.entity

import com.back.devc.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "comment_attachments")
class CommentAttachment protected constructor() : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    var id: Long? = null
        protected set

    @Column(name = "comment_id", nullable = false)
    var commentId: Long? = null
        protected set

    @Column(name = "file_name", nullable = false)
    var fileName: String? = null
        protected set

    @Column(name = "stored_name", nullable = false, unique = true)
    var storedName: String? = null
        protected set

    @Column(name = "file_url", nullable = false)
    var fileUrl: String? = null
        protected set

    @Column(name = "file_type", nullable = false, length = 20)
    var fileType: String? = null
        protected set

    @Column(name = "mime_type", nullable = false, length = 100)
    var mimeType: String? = null
        protected set

    @Column(name = "file_size", nullable = false)
    var fileSize: Long? = null
        protected set

    @Column(name = "file_order")
    var fileOrder: Int? = null
        protected set

    constructor(
        commentId: Long?,
        fileName: String?,
        storedName: String?,
        fileUrl: String?,
        fileType: String?,
        mimeType: String?,
        fileSize: Long?,
        fileOrder: Int?,
    ) : this() {
        this.commentId = commentId
        this.fileName = fileName
        this.storedName = storedName
        this.fileUrl = fileUrl
        this.fileType = fileType
        this.mimeType = mimeType
        this.fileSize = fileSize
        this.fileOrder = fileOrder
    }

    companion object {
        @JvmStatic
        fun create(
            commentId: Long?,
            fileName: String?,
            storedName: String?,
            fileUrl: String?,
            fileType: String?,
            mimeType: String?,
            fileSize: Long?,
            fileOrder: Int?,
        ): CommentAttachment {
            return CommentAttachment(
                commentId,
                fileName,
                storedName,
                fileUrl,
                fileType,
                mimeType,
                fileSize,
                fileOrder,
            )
        }
    }
}