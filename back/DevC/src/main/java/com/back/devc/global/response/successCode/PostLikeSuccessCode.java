package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostLikeSuccessCode implements SuccessCodeSpec {

    POST_LIKE_CREATED(HttpStatus.OK, "POST_LIKE_CREATED", "좋아요가 추가되었습니다."),
    POST_LIKE_ALREADY_EXISTS(HttpStatus.OK, "POST_LIKE_ALREADY_EXISTS", "이미 좋아요한 게시글입니다."),
    POST_LIKE_CANCELED(HttpStatus.OK, "POST_LIKE_CANCELED", "좋아요가 취소되었습니다."),
    POST_LIKE_ALREADY_CANCELED(HttpStatus.OK, "POST_LIKE_ALREADY_CANCELED", "좋아요가 이미 취소된 상태입니다."),
    LIKED_POSTS_FETCHED(HttpStatus.OK, "LIKED_POSTS_FETCHED", "좋아요한 게시글 목록을 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}