package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportSuccessCode implements SuccessCodeSpec {

    // --- 200 OK (조회 및 처리 완료) ---
    REPORT_200_LIST(HttpStatus.OK, "REPORT_200", "신고 내역 목록 조회에 성공하였습니다."),
    REPORT_200_GROUP_LIST(HttpStatus.OK, "REPORT_200", "그룹화된 신고 내역 조회에 성공하였습니다."),

    // 관리자 처리 관련 (Admin)
    REPORT_200_APPROVE(HttpStatus.OK, "REPORT_200", "신고 승인 및 제재 처리가 완료되었습니다."),
    REPORT_200_REJECT(HttpStatus.OK, "REPORT_200", "신고 반려 처리가 완료되었습니다."),
    REPORT_200_GROUP_APPROVE(HttpStatus.OK, "REPORT_200", "신고 그룹에 대한 일괄 승인이 완료되었습니다."),
    REPORT_200_GROUP_REJECT(HttpStatus.OK, "REPORT_200", "신고 그룹에 대한 일괄 반려가 완료되었습니다."),

    // --- 201 CREATED (신고 접수 완료) ---
    REPORT_201_POST(HttpStatus.CREATED, "REPORT_201", "게시글 신고가 정상적으로 접수되었습니다."),
    REPORT_201_COMMENT(HttpStatus.CREATED, "REPORT_201", "댓글 신고가 정상적으로 접수되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
