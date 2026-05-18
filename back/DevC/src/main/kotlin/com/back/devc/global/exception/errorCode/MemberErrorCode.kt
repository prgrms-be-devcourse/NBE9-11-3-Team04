package com.back.devc.global.exception.errorCode

import com.back.devc.global.exception.ErrorCodeSpec
import org.springframework.http.HttpStatus

enum class MemberErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCodeSpec {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404_NOT_FOUND", "회원을 찾을 수 없습니다.");
}