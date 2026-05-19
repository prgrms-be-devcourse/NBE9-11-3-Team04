package com.back.devc.global.exception.errorCode

import com.back.devc.global.exception.ErrorCodeSpec
import org.springframework.http.HttpStatus

enum class ReportErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCodeSpec {
    REPORT_400_REPORT_SELF(HttpStatus.BAD_REQUEST, "REPORT_400", "본인의 게시글이나 댓글은 신고할 수 없습니다."),
    REPORT_400_INVALID_SANCTION_PARAMETER(HttpStatus.BAD_REQUEST, "REPORT_400", "정지 기간 또는 제재 파라미터가 올바르지 않습니다."),

    REPORT_403_UNAUTHORIZED_ADMIN(HttpStatus.FORBIDDEN, "REPORT_403", "관리자 권한이 없습니다."),

    REPORT_404_REPORT(HttpStatus.NOT_FOUND, "REPORT_404", "해당 신고 내역을 찾을 수 없습니다."),
    REPORT_404_TARGET(HttpStatus.NOT_FOUND, "REPORT_404", "신고 대상 게시글/댓글을 찾을 수 없습니다."),
    REPORT_404_TARGET_USER(HttpStatus.NOT_FOUND, "REPORT_404", "신고 대상의 작성자를 찾을 수 없습니다."),
    REPORT_404_PENDING_LIST(HttpStatus.NOT_FOUND, "REPORT_404", "처리 가능한 대기 상태의 신고가 없습니다."),

    REPORT_409_ALREADY_REPORT_USER(HttpStatus.CONFLICT, "REPORT_409", "이미 신고한 대상입니다."),
    REPORT_409_ALREADY_REPORT(HttpStatus.CONFLICT, "REPORT_409", "이미 처리된 신고입니다."),
    REPORT_GROUP_409_ALREADY_REPORT(HttpStatus.CONFLICT, "REPORT_409", "이미 처리된 신고 그룹입니다."),

    REPORT_410_ALREADY_DELETED(HttpStatus.GONE, "REPORT_410", "이미 삭제된 대상은 신고할 수 없습니다."),
}
