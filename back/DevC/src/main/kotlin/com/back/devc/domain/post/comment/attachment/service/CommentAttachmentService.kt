package com.back.devc.domain.post.comment.attachment.service

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentDeleteResponse
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentResponse
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentUploadRequest
import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment
import com.back.devc.domain.post.comment.attachment.repository.CommentAttachmentRepository
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.CommentAttachmentErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CommentAttachmentService(
    private val commentAttachmentRepository: CommentAttachmentRepository,
    private val commentRepository: CommentRepository,
) {

    @Transactional
    fun uploadAttachments(
        commentId: Long,
        request: CommentAttachmentUploadRequest,
    ): CommentAttachmentListResponse {
        log.info("댓글 첨부파일 업로드 시작 - commentId={}", commentId)
        validateCommentExists(commentId)

        val files = request.files
        val fileOrders = request.fileOrders

        if (files.isEmpty()) {
            log.info("댓글 첨부파일 업로드 생략 - 첨부파일 없음, commentId={}", commentId)
            return CommentAttachmentListResponse(emptyList())
        }

        log.info("댓글 첨부파일 업로드 파일 수 확인 - commentId={}, fileCount={}", commentId, files.size)

        val responses = mutableListOf<CommentAttachmentResponse>()

        files.forEachIndexed { index, file ->
            if (file.isEmpty) {
                log.info("댓글 첨부파일 업로드 생략 - 빈 파일, commentId={}, index={}", commentId, index)
                return@forEachIndexed
            }

            val fileOrder = fileOrders.getOrNull(index) ?: index
            val originalFilename = file.originalFilename ?: "unnamed"
            val contentType = file.contentType ?: DEFAULT_CONTENT_TYPE
            val extension = extractExtension(originalFilename)
            val storedName = "${UUID.randomUUID()}$extension"
            val fileType = if (contentType.startsWith("image/")) "IMAGE" else "FILE"

            saveFile(file, storedName)
            log.debug(
                "댓글 첨부파일 물리 파일 저장 완료 - commentId={}, storedName={}, size={}, contentType={}",
                commentId,
                storedName,
                file.size,
                contentType,
            )

            val fileUrl = "/uploads/comments/$storedName"
            val attachment = CommentAttachment.create(
                commentId = commentId,
                fileName = originalFilename,
                storedName = storedName,
                fileUrl = fileUrl,
                fileType = fileType,
                mimeType = contentType,
                fileSize = file.size,
                fileOrder = fileOrder,
            )

            val savedAttachment = commentAttachmentRepository.save(attachment)
            log.info(
                "댓글 첨부파일 DB 저장 완료 - commentId={}, attachmentId={}, storedName={}, fileType={}, fileOrder={}",
                commentId,
                savedAttachment.id,
                savedAttachment.storedName,
                savedAttachment.fileType,
                savedAttachment.fileOrder,
            )

            responses.add(savedAttachment.toResponse())
        }

        log.info("댓글 첨부파일 업로드 완료 - commentId={}, savedCount={}", commentId, responses.size)
        return CommentAttachmentListResponse(responses)
    }

    fun getAttachments(commentId: Long): CommentAttachmentListResponse {
        log.info("댓글 첨부파일 목록 조회 시작 - commentId={}", commentId)
        validateCommentExists(commentId)

        val responses = commentAttachmentRepository.findByCommentIdOrderByFileOrderAscIdAsc(commentId)
            .map { attachment -> attachment.toResponse() }

        log.info("댓글 첨부파일 목록 조회 완료 - commentId={}, count={}", commentId, responses.size)
        return CommentAttachmentListResponse(responses)
    }

    @Transactional
    fun deleteAttachment(commentId: Long, attachmentId: Long): CommentAttachmentDeleteResponse {
        log.info("댓글 첨부파일 삭제 시작 - commentId={}, attachmentId={}", commentId, attachmentId)
        validateCommentExists(commentId)

        val attachment = commentAttachmentRepository.findByIdAndCommentId(attachmentId, commentId)
            ?: throw ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_404_NOT_FOUND)

        log.debug(
            "댓글 첨부파일 삭제 대상 조회 완료 - commentId={}, attachmentId={}, storedName={}",
            commentId,
            attachmentId,
            attachment.storedName,
        )

        deleteFileIfExists(requireNotNull(attachment.storedName))
        commentAttachmentRepository.delete(attachment)

        log.info("댓글 첨부파일 삭제 완료 - commentId={}, attachmentId={}", commentId, attachmentId)
        return CommentAttachmentDeleteResponse(attachmentId, "댓글 첨부파일 삭제 성공")
    }

    private fun validateCommentExists(commentId: Long) {
        commentRepository.findById(commentId)
            .orElseThrow { ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_404_COMMENT_NOT_FOUND) }
    }

    private fun extractExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')

        if (lastDotIndex == -1 || lastDotIndex == fileName.length - 1) {
            return ""
        }

        return fileName.substring(lastDotIndex)
    }

    private fun saveFile(file: MultipartFile, storedName: String) {
        try {
            Files.createDirectories(COMMENT_UPLOAD_DIR)
            val targetPath = COMMENT_UPLOAD_DIR.resolve(storedName)
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            log.error(
                "댓글 첨부파일 저장 실패 - storedName={}, uploadDir={}",
                storedName,
                COMMENT_UPLOAD_DIR,
                e,
            )
            throw ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_500_SAVE_FAILED)
        }
    }

    private fun deleteFileIfExists(storedName: String) {
        try {
            val targetPath = COMMENT_UPLOAD_DIR.resolve(storedName)
            log.debug("댓글 첨부파일 물리 파일 삭제 시도 - storedName={}, path={}", storedName, targetPath)
            val deleted = Files.deleteIfExists(targetPath)
            log.info("댓글 첨부파일 물리 파일 삭제 결과 - storedName={}, deleted={}", storedName, deleted)
        } catch (e: IOException) {
            log.error(
                "댓글 첨부파일 삭제 실패 - storedName={}, uploadDir={}",
                storedName,
                COMMENT_UPLOAD_DIR,
                e,
            )
            throw ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_500_DELETE_FAILED)
        }
    }

    private fun CommentAttachment.toResponse(): CommentAttachmentResponse {
        return CommentAttachmentResponse(
            requireNotNull(id),
            requireNotNull(commentId),
            requireNotNull(fileName),
            requireNotNull(storedName),
            requireNotNull(fileUrl),
            requireNotNull(fileType),
            requireNotNull(mimeType),
            requireNotNull(fileSize),
            requireNotNull(fileOrder),
            createdAt,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommentAttachmentService::class.java)
        private val COMMENT_UPLOAD_DIR: Path = Paths.get("uploads", "comments")
        private const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
    }
}