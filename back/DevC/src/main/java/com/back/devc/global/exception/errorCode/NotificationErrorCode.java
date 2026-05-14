package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * NotificationService 기준 실제 로직에서 필요한 알림 관련 에러 코드.
 *
 * 현재 확인한 실제 예외 케이스
 * - 알림이 존재하지 않음
 * - 본인 알림이 아닌 경우 읽음 처리 불가
 * - 게시글이 존재하지 않음
 * - 댓글이 존재하지 않음
 * - 부모 댓글이 존재하지 않음
 * - 회원이 존재하지 않음
 */
@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCodeSpec {

    NOTIFICATION_404_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NOTIFICATION_404_NOT_FOUND",
            "알림을 찾을 수 없습니다."
    ),
    NOTIFICATION_403_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "NOTIFICATION_403_FORBIDDEN",
            "본인의 알림만 읽음 처리할 수 있습니다."
    ),
    NOTIFICATION_404_POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NOTIFICATION_404_POST_NOT_FOUND",
            "게시글을 찾을 수 없습니다."
    ),
    NOTIFICATION_404_COMMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NOTIFICATION_404_COMMENT_NOT_FOUND",
            "댓글을 찾을 수 없습니다."
    ),
    NOTIFICATION_404_PARENT_COMMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NOTIFICATION_404_PARENT_COMMENT_NOT_FOUND",
            "부모 댓글을 찾을 수 없습니다."
    ),
    NOTIFICATION_404_MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NOTIFICATION_404_MEMBER_NOT_FOUND",
            "회원을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
