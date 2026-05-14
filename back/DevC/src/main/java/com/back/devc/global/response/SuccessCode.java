package com.back.devc.global.response;

import org.springframework.http.HttpStatus;

public enum SuccessCode {
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH_200_LOGIN_SUCCESS", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_200_LOGOUT_SUCCESS", "로그아웃이 완료되었습니다."),
    SIGN_UP_SUCCESS(HttpStatus.CREATED, "AUTH_201_SIGNUP_SUCCESS", "회원가입이 완료되었습니다."),

    MEMBER_LIST_SUCCESS(HttpStatus.OK, "ADM_200_MEMBER_LIST", "회원 목록 조회가 완료되었습니다."),
    MEMBER_DETAIL_SUCCESS(HttpStatus.OK, "ADM_200_MEMBER_DETAIL", "회원 상세 정보 조회가 완료되었습니다."),
    MEMBER_STATUS_UPDATE_SUCCESS(HttpStatus.OK, "ADM_200_MEMBER_STATUS_UPDATE", "회원 상태 변경이 완료되었습니다."),
    MEMBER_SEARCH_SUCCESS(HttpStatus.OK, "ADM_200_MEMBER_SEARCH", "회원 검색이 완료되었습니다."),

    // 신고 관련 성공 코드
    REPORT_LIST_SUCCESS(HttpStatus.OK, "REPORT_200_LIST", "신고 목록 조회 성공"),
    REPORT_GROUP_LIST_SUCCESS(HttpStatus.OK, "REPORT_200_GROUP_LIST", "그룹 신고 조회 성공"),
    REPORT_GROUP_APPROVE_SUCCESS(HttpStatus.OK, "REPORT_200_GROUP_APPROVE", "그룹 신고 승인 완료"),
    REPORT_GROUP_REJECT_SUCCESS(HttpStatus.OK, "REPORT_200_GROUP_REJECT", "그룹 신고 반려 완료"),

    REPORT_POST_SUCCESS(HttpStatus.CREATED, "REPORT_201_POST", "게시글 신고가 정상적으로 접수되었습니다."),
    REPORT_COMMENT_SUCCESS(HttpStatus.CREATED, "REPORT_201_COMMENT", "댓글 신고가 정상적으로 접수되었습니다."),


    // 대시보드 관련 성공 코드
    DASHBOARD_LIST(HttpStatus.OK, "DASHBOARD_200_LIST", "대시보드 조회 성공"),

    // 회원 탈퇴 성공 코드
    WITHDRAW_SUCCESS(HttpStatus.OK, "USER_200_WITHDRAW_SUCCESS", "회원 탈퇴가 완료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SuccessCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
