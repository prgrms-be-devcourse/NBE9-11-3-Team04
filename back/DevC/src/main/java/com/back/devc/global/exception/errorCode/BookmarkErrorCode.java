package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

public enum BookmarkErrorCode implements ErrorCodeSpec {

    BOOKMARK_404_MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "BOOKMARK_404_MEMBER_NOT_FOUND",
            "회원을 찾을 수 없습니다."
    ),

    BOOKMARK_404_POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "BOOKMARK_404_POST_NOT_FOUND",
            "게시글을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    BookmarkErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}