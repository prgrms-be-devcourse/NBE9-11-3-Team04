package com.back.devc.global.response.successCode

import com.back.devc.global.response.SuccessCodeSpec
import org.springframework.http.HttpStatus

enum class BookmarkSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCodeSpec {
    BOOKMARK_201_CREATE(
        HttpStatus.CREATED,
        "BOOKMARK_201_CREATE",
        "북마크가 추가되었습니다."
    ),

    BOOKMARK_200_DELETE(
        HttpStatus.OK,
        "BOOKMARK_200_DELETE",
        "북마크가 취소되었습니다."
    ),

    BOOKMARK_200_READ_LIST(
        HttpStatus.OK,
        "BOOKMARK_200_READ_LIST",
        "북마크 목록 조회에 성공했습니다."
    );
}