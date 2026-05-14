package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements SuccessCodeSpec {
    NOTIFICATION_200_LIST(HttpStatus.OK, "NOTIFICATION_200_LIST", "알림 목록 조회 성공"),
    NOTIFICATION_200_READ(HttpStatus.OK, "NOTIFICATION_200_READ", "알림 읽음 처리 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;
}