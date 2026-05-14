package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * CommentService 기준 실제 로직에서 필요한 댓글 관련 에러 코드.
 *
 * 현재 확인한 실제 예외 케이스
 * - 댓글이 존재하지 않음
 * - 부모 댓글이 존재하지 않음
 * - 게시글이 존재하지 않음
 * - 회원이 존재하지 않음
 * - 삭제된 댓글에는 답글 작성 불가
 * - 본인이 작성한 댓글만 수정/삭제 가능
 *
 */
@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCodeSpec {

    COMMENT_404_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMENT_404_NOT_FOUND",
            "댓글을 찾을 수 없습니다."
    ),
    COMMENT_404_PARENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMENT_404_PARENT_NOT_FOUND",
            "부모 댓글을 찾을 수 없습니다."
    ),
    COMMENT_404_POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMENT_404_POST_NOT_FOUND",
            "게시글을 찾을 수 없습니다."
    ),
    COMMENT_404_MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMENT_404_MEMBER_NOT_FOUND",
            "회원을 찾을 수 없습니다."
    ),
    COMMENT_400_REPLY_TO_DELETED_COMMENT(
            HttpStatus.BAD_REQUEST,
            "COMMENT_400_REPLY_TO_DELETED_COMMENT",
            "삭제된 댓글에는 답글을 작성할 수 없습니다."
    ),
    COMMENT_403_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "COMMENT_403_FORBIDDEN",
            "본인이 작성한 댓글만 수정/삭제할 수 있습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
