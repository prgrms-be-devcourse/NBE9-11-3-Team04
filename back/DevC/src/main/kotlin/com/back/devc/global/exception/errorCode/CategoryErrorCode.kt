package com.back.devc.global.exception.errorCode

import com.back.devc.global.exception.ErrorCodeSpec
import org.springframework.http.HttpStatus

enum class CategoryErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCodeSpec {
    CATEGORY_404_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "CATEGORY_404_NOT_FOUND",
        "카테고리를 찾을 수 없습니다."
    );
}