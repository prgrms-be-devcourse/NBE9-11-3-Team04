package com.back.devc.global.response.successCode

import com.back.devc.global.response.SuccessCodeSpec
import org.springframework.http.HttpStatus

enum class NotificationSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCodeSpec {
    NOTIFICATION_200_LIST(HttpStatus.OK, "NOTIFICATION_200_LIST", "알림 목록 조회 성공"),
    NOTIFICATION_200_READ(HttpStatus.OK, "NOTIFICATION_200_READ", "알림 읽음 처리 성공");
}