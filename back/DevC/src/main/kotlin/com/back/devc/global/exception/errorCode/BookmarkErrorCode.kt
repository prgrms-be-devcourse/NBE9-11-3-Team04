package com.back.devc.global.exception.errorCode

import com.back.devc.global.exception.ErrorCodeSpec
import org.springframework.http.HttpStatus

enum class BookmarkErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCodeSpec {
    BOOKMARK_404_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK_404_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    BOOKMARK_404_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK_404_POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
}