package com.back.devc.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인별로 분리된 에러 코드 enum 을 공통 방식으로 처리하기 위한 인터페이스.
 *
 * status  : HTTP 상태 코드
 * code    : 에러 코드 식별자
 * message : 에러 메시지
 */
public interface ErrorCodeSpec {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
