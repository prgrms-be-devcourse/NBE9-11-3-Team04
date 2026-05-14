package com.back.devc.global.response;

import com.back.devc.global.response.successCode.AuthSuccessCode;
import com.back.devc.global.response.successCode.MemberSuccessCode;
import com.back.devc.global.response.successCode.ReportSuccessCode;

import java.time.LocalDateTime;

/**
 * API 성공 응답을 공통 형식으로 내려주기 위한 Response DTO.
 *
 * code      : 성공 코드 식별자
 * message   : 성공 메시지
 * timestamp : 응답 생성 시각
 * data      : 실제 응답 데이터 본문
 *
 * 기존 SuccessCode/AuthSuccessCode/MemberSuccessCode 구조와의 호환을 유지하면서,
 * 분리된 SuccessCode enum 들도 SuccessCodeSpec 인터페이스를 통해 공통 처리할 수 있도록 구성한다.
 */
public record SuccessResponse<T>(
        String code,
        String message,
        LocalDateTime timestamp,
        T data
) {
    /**
     * 기존 공통 SuccessCode enum 을 사용하는 성공 응답 생성 메서드.
     */
    public static <T> SuccessResponse<T> of(SuccessCode successCode, T data) {
        return new SuccessResponse<>(
                successCode.getCode(),
                successCode.getMessage(),
                LocalDateTime.now(),
                data
        );
    }

    /**
     * 회원 도메인 전용 SuccessCode enum 을 사용하는 성공 응답 생성 메서드.
     */
    public static <T> SuccessResponse<T> of(MemberSuccessCode successCode, T data) {
        return new SuccessResponse<>(
                successCode.getCode(),
                successCode.getMessage(),
                LocalDateTime.now(),
                data
        );
    }

    /**
     * 인증 도메인 전용 SuccessCode enum 을 사용하는 성공 응답 생성 메서드.
     */
    public static <T> SuccessResponse<T> of(AuthSuccessCode successCode, T data) {
        return new SuccessResponse<>(
                successCode.getCode(),
                successCode.getMessage(),
                LocalDateTime.now(),
                data
        );
    }

    /**
     * 분리된 SuccessCode enum 들이 SuccessCodeSpec 을 구현했을 때 공통으로 사용하는 성공 응답 생성 메서드.
     *
     * 예: CommentSuccessCode, NotificationSuccessCode, CommentAttachmentSuccessCode
     */
    public static <T> SuccessResponse<T> of(SuccessCodeSpec successCode, T data) {
        return new SuccessResponse<>(
                successCode.getCode(),
                successCode.getMessage(),
                LocalDateTime.now(),
                data
        );
    }

    //예비
    public static <T> SuccessResponse<T> of(ReportSuccessCode successCode, T data) {
        return new SuccessResponse<>(
                successCode.getCode(),
                successCode.getMessage(),
                LocalDateTime.now(),
                data
        );
    }

    /**
     * enum 을 사용하지 않고 code/message 를 직접 지정해야 하는 경우 사용하는 성공 응답 생성 메서드.
     */
    public static <T> SuccessResponse<T> of(String code, String message, T data) {
        return new SuccessResponse<>(code, message, LocalDateTime.now(), data);
    }
}
