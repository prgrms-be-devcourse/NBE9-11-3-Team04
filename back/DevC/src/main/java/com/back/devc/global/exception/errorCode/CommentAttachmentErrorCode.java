package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * CommentAttachmentService / Controller 기준 실제 로직에서 필요한 댓글 첨부파일 관련 에러 코드.
 *
 * 현재 확인한 실제 예외 케이스
 * - 댓글이 존재하지 않음
 * - 삭제 대상 첨부파일이 존재하지 않음
 * - 파일 저장 실패
 * - 파일 삭제 실패
 *
 * 현재 로직에는 아래 항목이 없음
 * - 첨부파일 권한 체크
 * - fileOrder 유효성 검증
 * - 빈 파일 업로드 예외 (빈 리스트는 정상적으로 빈 응답 반환)
 */
@Getter
@RequiredArgsConstructor
public enum CommentAttachmentErrorCode implements ErrorCodeSpec {

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

    private final HttpStatus status;
    private final String code;
    private final String message;
}
