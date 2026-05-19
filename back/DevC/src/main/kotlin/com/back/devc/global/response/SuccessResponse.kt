package com.back.devc.global.response

import com.back.devc.global.response.successCode.AuthSuccessCode
import com.back.devc.global.response.successCode.MemberSuccessCode
import java.time.LocalDateTime

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
data class SuccessResponse<T>(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime,
    val data: T?
) {
    companion object {
        /**
         * 기존 공통 SuccessCode enum 을 사용하는 성공 응답 생성 메서드.
         */
        @JvmStatic
        fun <T> of(successCode: SuccessCode, data: T?): SuccessResponse<T> {
            return SuccessResponse(
                code = successCode.code,
                message = successCode.message,
                timestamp = LocalDateTime.now(),
                data = data
            )
        }

        /**
         * 회원 도메인 전용 SuccessCode enum 을 사용하는 성공 응답 생성 메서드.
         */
        @JvmStatic
        fun <T> of(successCode: MemberSuccessCode, data: T?): SuccessResponse<T> {
            return SuccessResponse(
                code = successCode.code,
                message = successCode.message,
                timestamp = LocalDateTime.now(),
                data = data
            )
        }

        /**
         * 인증 도메인 전용 SuccessCode enum 을 사용하는 성공 응답 생성 메서드.
         */
        @JvmStatic
        fun <T> of(successCode: AuthSuccessCode, data: T?): SuccessResponse<T> {
            return SuccessResponse(
                code = successCode.code,
                message = successCode.message,
                timestamp = LocalDateTime.now(),
                data = data
            )
        }

        /**
         * 분리된 SuccessCode enum 들이 SuccessCodeSpec 을 구현했을 때 공통으로 사용하는 성공 응답 생성 메서드.
         *
         * 예: CommentSuccessCode, NotificationSuccessCode, CommentAttachmentSuccessCode
         */
        @JvmStatic
        fun <T> of(successCode: SuccessCodeSpec, data: T?): SuccessResponse<T> {
            return SuccessResponse(
                code = successCode.code,
                message = successCode.message,
                timestamp = LocalDateTime.now(),
                data = data
            )
        }

        /**
         * enum 을 사용하지 않고 code/message 를 직접 지정해야 하는 경우 사용하는 성공 응답 생성 메서드.
         */
        @JvmStatic
        fun <T> of(code: String, message: String, data: T?): SuccessResponse<T> {
            return SuccessResponse(
                code = code,
                message = message,
                timestamp = LocalDateTime.now(),
                data = data
            )
        }
    }
}