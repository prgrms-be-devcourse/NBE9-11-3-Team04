package com.back.devc.global.exception.errorCode;
import com.back.devc.global.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCodeSpec {
    POST_404_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "POST_404_NOT_FOUND",
            "게시글을 찾을 수 없습니다."),

    POST_401_1_ALREADY_DELETED(
            HttpStatus.BAD_REQUEST,
            "POST_401_1_ALREADY_DELETED",
            "이미 삭제된 게시글입니다."),

    POST_403_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "POST_403_FORBIDDEN",
            "권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
