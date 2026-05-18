package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BookmarkSuccessCode implements SuccessCodeSpec {

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

    private final HttpStatus status;
    private final String code;
    private final String message;
}