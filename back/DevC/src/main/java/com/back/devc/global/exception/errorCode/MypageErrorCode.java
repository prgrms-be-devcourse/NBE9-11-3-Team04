package com.back.devc.global.exception.errorcode;

import org.springframework.http.HttpStatus;

public enum MypageErrorCode {

    MYPAGE_404_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MYPAGE_404_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    MYPAGE_409_NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "MYPAGE_409_NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MypageErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}