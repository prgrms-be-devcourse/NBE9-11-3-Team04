package com.back.devc.global.response.successCode;
import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostSuccessCode implements SuccessCodeSpec {

    POST_201_CREATE_SUCCESS(HttpStatus.CREATED,"POST_201_CREATE_SUCCESS", "게시글 작성 성공"),
    POST_200_LIST_SUCCESS(HttpStatus.OK,"POST_200_LIST_SUCCESS", "게시글 목록 조회 성공"),
    POST_200_DETAIL_SUCCESS(HttpStatus.OK,"POST_200_DETAIL_SUCCESS", "게시글 상세 조회 성공"),
    POST_200_UPDATE_SUCCESS(HttpStatus.OK,"POST_200_UPDATE_SUCCESS", "게시글 수정 성공"),
    POST_200_DELETE_SUCCESS(HttpStatus.OK,"POST_200_DELETE_SUCCESS", "게시글 삭제 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
