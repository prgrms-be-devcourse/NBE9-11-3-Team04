package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCodeSpec {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404_NOT_FOUND", "회원을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
