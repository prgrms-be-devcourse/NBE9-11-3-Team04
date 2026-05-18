package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

public enum MypageErrorCode implements ErrorCodeSpec {

    MYPAGE_404_MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "MYPAGE_404_MEMBER_NOT_FOUND",
            "회원을 찾을 수 없습니다."
    ),

    MYPAGE_404_POST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "MYPAGE_404_POST_NOT_FOUND",
            "게시글을 찾을 수 없습니다."
    ),

    MYPAGE_409_NICKNAME_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "MYPAGE_409_NICKNAME_ALREADY_EXISTS",
            "이미 사용 중인 닉네임입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    MypageErrorCode(HttpStatus status, String code, String message) {
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