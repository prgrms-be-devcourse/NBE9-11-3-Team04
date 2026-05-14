package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCodeSpec {

    // --- 400 BAD REQUEST (클라이언트 요청 오류) ---
    REPORT_400_REPORT_SELF(HttpStatus.BAD_REQUEST, "REPORT_400", "본인의 게시글이나 댓글은 신고할 수 없습니다."),
    REPORT_400_INVALID_SANCTION_PARAMETER(HttpStatus.BAD_REQUEST, "REPORT_400", "정지 기간 등 제재 파라미터가 올바르지 않습니다."),

    // --- 403 FORBIDDEN (권한 오류) ---
    REPORT_403_UNAUTHORIZED_ADMIN(HttpStatus.FORBIDDEN, "REPORT_403", "관리자 권한이 없습니다."),

    // --- 404 NOT FOUND (자원 없음) ---
    REPORT_404_REPORT(HttpStatus.NOT_FOUND, "REPORT_404", "해당 신고 내역을 찾을 수 없습니다."),
    REPORT_404_TARGET(HttpStatus.NOT_FOUND, "REPORT_404", "신고 대상(게시글/댓글)을 찾을 수 없습니다."), // POST_404, COMMENT_404 통합 추천
    REPORT_404_TARGET_USER(HttpStatus.NOT_FOUND, "REPORT_404", "신고 대상의 작성자를 찾을 수 없습니다."),
    REPORT_404_PENDING_LIST(HttpStatus.NOT_FOUND, "REPORT_404", "처리할 수 있는 대기 상태의 신고가 없습니다."),

    // --- 409 CONFLICT (상태 충돌) ---
    REPORT_409_ALREADY_REPORT_USER(HttpStatus.CONFLICT, "REPORT_409", "이미 신고한 대상입니다."),
    REPORT_409_ALREADY_REPORT(HttpStatus.CONFLICT, "REPORT_409", "이미 처리된 신고입니다."),

    // --- 410 GONE (삭제) ---
    REPORT_410_ALREADY_DELETED(HttpStatus.GONE, "REPORT_410", "이미 삭제된 대상은 신고할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}