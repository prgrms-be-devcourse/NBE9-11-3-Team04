package com.back.devc.global.exception.errorCode

import com.back.devc.global.exception.ErrorCodeSpec
import org.springframework.http.HttpStatus

enum class CommentAttachmentErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCodeSpec {
    COMMENT_ATTACHMENT_404_COMMENT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "COMMENT_ATTACHMENT_404_COMMENT_NOT_FOUND",
        "댓글을 찾을 수 없습니다."
    ),
    COMMENT_ATTACHMENT_404_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "COMMENT_ATTACHMENT_404_NOT_FOUND",
        "댓글 첨부파일을 찾을 수 없습니다."
    ),
    COMMENT_ATTACHMENT_500_SAVE_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "COMMENT_ATTACHMENT_500_SAVE_FAILED",
        "댓글 첨부파일 저장에 실패했습니다."
    ),
    COMMENT_ATTACHMENT_500_DELETE_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "COMMENT_ATTACHMENT_500_DELETE_FAILED",
        "댓글 첨부파일 삭제에 실패했습니다."
    );
}