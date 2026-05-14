package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import org.springframework.http.HttpStatus;

public enum BookmarkSuccessCode implements SuccessCodeSpec {

    BOOKMARK_201_CREATE(HttpStatus.CREATED, "BOOKMARK_201_CREATE", "북마크가 추가되었습니다."),
    BOOKMARK_200_DELETE(HttpStatus.OK, "BOOKMARK_200_DELETE", "북마크가 취소되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BookmarkSuccessCode(HttpStatus status, String code, String message) {
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