package com.back.devc.global.response.successCode

import com.back.devc.global.response.SuccessCodeSpec
import org.springframework.http.HttpStatus

enum class PostSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCodeSpec {
    POST_201_CREATE_SUCCESS(HttpStatus.CREATED, "POST_201_CREATE_SUCCESS", "게시글 작성 성공"),
    POST_200_LIST_SUCCESS(HttpStatus.OK, "POST_200_LIST_SUCCESS", "게시글 목록 조회 성공"),
    POST_200_DETAIL_SUCCESS(HttpStatus.OK, "POST_200_DETAIL_SUCCESS", "게시글 상세 조회 성공"),
    POST_200_UPDATE_SUCCESS(HttpStatus.OK, "POST_200_UPDATE_SUCCESS", "게시글 수정 성공"),
    POST_200_DELETE_SUCCESS(HttpStatus.OK, "POST_200_DELETE_SUCCESS", "게시글 삭제 성공");
}