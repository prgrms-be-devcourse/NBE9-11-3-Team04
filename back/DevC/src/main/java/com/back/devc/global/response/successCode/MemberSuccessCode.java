package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum MemberSuccessCode implements SuccessCodeSpec {
    MEMBER_200_ME_SUCCESS(HttpStatus.OK, "MEMBER_200_ME_SUCCESS", "내 정보 조회에 성공했습니다."),
    MEMBER_200_PUBLIC_PROFILE_GET_SUCCESS(HttpStatus.OK, "MEMBER_200_PUBLIC_PROFILE_GET_SUCCESS", "공개 프로필 조회에 성공했습니다."),
    MEMBER_200_WITHDRAW_SUCCESS(HttpStatus.OK, "MEMBER_200_WITHDRAW_SUCCESS", "회원 탈퇴가 완료되었습니다."),

    // 관리자 관련 성공 코드
    ADMIN_MEMBER_LIST_SUCCESS(HttpStatus.OK, "ADMIN_MEMBER_200_LIST_SUCCESS", "회원 목록 조회에 성공했습니다."),
    ADMIN_MEMBER_DETAIL_SUCCESS(HttpStatus.OK, "ADMIN_MEMBER_200_DETAIL_SUCCESS", "회원 상세 조회에 성공했습니다."),
    ADMIN_MEMBER_STATUS_UPDATE_SUCCESS(HttpStatus.OK, "ADMIN_MEMBER_200_STATUS_UPDATE_SUCCESS", "회원 상태 수정에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}