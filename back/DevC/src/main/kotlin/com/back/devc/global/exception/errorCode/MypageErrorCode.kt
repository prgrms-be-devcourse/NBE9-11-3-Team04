package com.back.devc.global.exception.errorCode

import com.back.devc.global.exception.ErrorCodeSpec
import org.springframework.http.HttpStatus

enum class MypageErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCodeSpec {
    MYPAGE_404_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MYPAGE_404_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    MYPAGE_409_NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "MYPAGE_409_NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다.");
}