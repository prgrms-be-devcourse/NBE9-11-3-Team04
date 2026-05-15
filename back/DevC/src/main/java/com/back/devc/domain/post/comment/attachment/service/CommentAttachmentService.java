package com.back.devc.domain.post.comment.attachment.service;

import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentDeleteResponse;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentListResponse;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentResponse;
import com.back.devc.domain.post.comment.attachment.dto.CommentAttachmentUploadRequest;
import com.back.devc.domain.post.comment.attachment.entity.CommentAttachment;
import com.back.devc.domain.post.comment.attachment.repository.CommentAttachmentRepository;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.CommentAttachmentErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentAttachmentService {

    private final CommentAttachmentRepository commentAttachmentRepository;
    private final CommentRepository commentRepository;
    private static final Path COMMENT_UPLOAD_DIR = Paths.get("uploads", "comments");

    @Transactional
    public CommentAttachmentListResponse uploadAttachments(
            Long commentId,
            CommentAttachmentUploadRequest request
    ) {
        log.info("댓글 첨부파일 업로드 시작 - commentId={}", commentId);
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_404_COMMENT_NOT_FOUND));

        List<MultipartFile> files = request.files();
        List<Integer> fileOrders = request.fileOrders();

        if (files == null || files.isEmpty()) {
            log.info("댓글 첨부파일 업로드 생략 - 첨부파일 없음, commentId={}", commentId);
            return new CommentAttachmentListResponse(List.of());
        }

        log.info("댓글 첨부파일 업로드 파일 수 확인 - commentId={}, fileCount={}", commentId, files.size());

        List<CommentAttachmentResponse> responses = new java.util.ArrayList<>();

        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            if (file == null || file.isEmpty()) {
                log.info("댓글 첨부파일 업로드 생략 - 빈 파일, commentId={}, index={}", commentId, index);
                continue;
            }
            Integer fileOrder = (fileOrders != null && fileOrders.size() > index)
                    ? fileOrders.get(index)
                    : index;

            String originalFilename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "unnamed";
            String contentType = file.getContentType() != null
                    ? file.getContentType()
                    : "application/octet-stream";
            String extension = extractExtension(originalFilename);
            String storedName = UUID.randomUUID() + extension;
            String fileType = contentType.startsWith("image/") ? "IMAGE" : "FILE";
            saveFile(file, storedName);
            log.debug("댓글 첨부파일 물리 파일 저장 완료 - commentId={}, storedName={}, size={}, contentType={}",
                    commentId,
                    storedName,
                    file.getSize(),
                    contentType);
            String fileUrl = "/uploads/comments/" + storedName;

            CommentAttachment attachment = CommentAttachment.create(
                    commentId,
                    originalFilename,
                    storedName,
                    fileUrl,
                    fileType,
                    contentType,
                    file.getSize(),
                    fileOrder
            );

            CommentAttachment savedAttachment = commentAttachmentRepository.save(attachment);
            log.info("댓글 첨부파일 DB 저장 완료 - commentId={}, attachmentId={}, storedName={}, fileType={}, fileOrder={}",
                    commentId,
                    savedAttachment.getId(),
                    savedAttachment.getStoredName(),
                    savedAttachment.getFileType(),
                    savedAttachment.getFileOrder());

            responses.add(new CommentAttachmentResponse(
                    savedAttachment.getId(),
                    savedAttachment.getCommentId(),
                    savedAttachment.getFileName(),
                    savedAttachment.getStoredName(),
                    savedAttachment.getFileUrl(),
                    savedAttachment.getFileType(),
                    savedAttachment.getMimeType(),
                    savedAttachment.getFileSize(),
                    savedAttachment.getFileOrder(),
                    savedAttachment.getCreatedAt()
            ));
        }
        log.info("댓글 첨부파일 업로드 완료 - commentId={}, savedCount={}", commentId, responses.size());
        return new CommentAttachmentListResponse(responses);
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex);
    }

    private void saveFile(MultipartFile file, String storedName) {
        try {
            Files.createDirectories(COMMENT_UPLOAD_DIR);
            Path targetPath = COMMENT_UPLOAD_DIR.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("댓글 첨부파일 저장 실패 - storedName={}, uploadDir={}", storedName, COMMENT_UPLOAD_DIR, e);
            throw new ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_500_SAVE_FAILED);
        }
    }

    private void deleteFileIfExists(String storedName) {
        try {
            Path targetPath = COMMENT_UPLOAD_DIR.resolve(storedName);
            log.debug("댓글 첨부파일 물리 파일 삭제 시도 - storedName={}, path={}", storedName, targetPath);
            boolean deleted = Files.deleteIfExists(targetPath);
            log.info("댓글 첨부파일 물리 파일 삭제 결과 - storedName={}, deleted={}", storedName, deleted);
        } catch (IOException e) {
            log.error("댓글 첨부파일 삭제 실패 - storedName={}, uploadDir={}", storedName, COMMENT_UPLOAD_DIR, e);
            throw new ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_500_DELETE_FAILED);
        }
    }

    public CommentAttachmentListResponse getAttachments(Long commentId) {
        log.info("댓글 첨부파일 목록 조회 시작 - commentId={}", commentId);
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_404_COMMENT_NOT_FOUND));

        List<CommentAttachmentResponse> responses = commentAttachmentRepository.findByCommentIdOrderByFileOrderAscIdAsc(commentId)
                .stream()
                .map(attachment -> new CommentAttachmentResponse(
                        attachment.getId(),
                        attachment.getCommentId(),
                        attachment.getFileName(),
                        attachment.getStoredName(),
                        attachment.getFileUrl(),
                        attachment.getFileType(),
                        attachment.getMimeType(),
                        attachment.getFileSize(),
                        attachment.getFileOrder(),
                        attachment.getCreatedAt()
                ))
                .toList();
        log.info("댓글 첨부파일 목록 조회 완료 - commentId={}, count={}", commentId, responses.size());
        return new CommentAttachmentListResponse(responses);
    }

    @Transactional
    public CommentAttachmentDeleteResponse deleteAttachment(Long commentId, Long attachmentId) {
        log.info("댓글 첨부파일 삭제 시작 - commentId={}, attachmentId={}", commentId, attachmentId);
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_404_COMMENT_NOT_FOUND));

        CommentAttachment attachment = commentAttachmentRepository.findByIdAndCommentId(attachmentId, commentId)
                .orElseThrow(() -> new ApiException(CommentAttachmentErrorCode.COMMENT_ATTACHMENT_404_NOT_FOUND));
        log.debug("댓글 첨부파일 삭제 대상 조회 완료 - commentId={}, attachmentId={}, storedName={}",
                commentId,
                attachmentId,
                attachment.getStoredName());

        deleteFileIfExists(attachment.getStoredName());
        commentAttachmentRepository.delete(attachment);
        log.info("댓글 첨부파일 삭제 완료 - commentId={}, attachmentId={}", commentId, attachmentId);
        return new CommentAttachmentDeleteResponse(attachmentId, "댓글 첨부파일 삭제 성공");
    }
}
