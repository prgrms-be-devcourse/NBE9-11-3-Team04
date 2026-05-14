package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import org.springframework.http.HttpStatus;

public enum MypageSuccessCode implements SuccessCodeSpec {

    MYPAGE_200_PROFILE_FETCH(HttpStatus.OK, "MYPAGE_200_PROFILE_FETCH", "내 프로필을 조회했습니다."),
    MYPAGE_200_POSTS_FETCH(HttpStatus.OK, "MYPAGE_200_POSTS_FETCH", "내 게시글 목록을 조회했습니다."),
    MYPAGE_200_COMMENTS_FETCH(HttpStatus.OK, "MYPAGE_200_COMMENTS_FETCH", "내 댓글 목록을 조회했습니다."),
    MYPAGE_200_LIKES_FETCH(HttpStatus.OK, "MYPAGE_200_LIKES_FETCH", "내 좋아요 게시글 목록을 조회했습니다."),
    MYPAGE_200_BOOKMARKS_FETCH(HttpStatus.OK, "MYPAGE_200_BOOKMARKS_FETCH", "내 북마크 게시글 목록을 조회했습니다."),
    MYPAGE_200_PROFILE_UPDATE(HttpStatus.OK, "MYPAGE_200_PROFILE_UPDATE", "내 프로필을 수정했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MypageSuccessCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}