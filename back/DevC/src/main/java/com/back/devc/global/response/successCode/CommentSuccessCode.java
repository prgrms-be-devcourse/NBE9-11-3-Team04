package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentSuccessCode implements SuccessCodeSpec {
    COMMENT_201_CREATE(HttpStatus.CREATED, "COMMENT_201_CREATE", "댓글 작성 성공"),
    COMMENT_201_REPLY(HttpStatus.CREATED, "COMMENT_201_REPLY", "대댓글 작성 성공"),
    COMMENT_200_UPDATE(HttpStatus.OK, "COMMENT_200_UPDATE", "댓글 수정 성공"),
    COMMENT_200_DELETE(HttpStatus.OK, "COMMENT_200_DELETE", "댓글 삭제 성공"),
    COMMENT_200_LIST(HttpStatus.OK, "COMMENT_200_LIST", "댓글 목록 조회 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
