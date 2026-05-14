package com.back.devc.global.response.successCode;

import com.back.devc.global.response.SuccessCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements SuccessCodeSpec {
    AUTH_200_LOGIN_SUCCESS(HttpStatus.OK, "AUTH_200_LOGIN_SUCCESS", "로그인에 성공했습니다."),
    AUTH_200_LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_200_LOGOUT_SUCCESS", "로그아웃이 완료되었습니다."),
    AUTH_201_SIGNUP_SUCCESS(HttpStatus.CREATED, "AUTH_201_SIGNUP_SUCCESS", "회원가입이 완료되었습니다."),

    OAUTH_200_ME_SUCCESS(HttpStatus.OK, "AUTH_200_OAUTH2_ME_SUCCESS", "OAuth2 사용자 정보 조회에 성공했습니다."),
    OAUTH_200_EXCHANGE_SUCCESS(HttpStatus.OK, "AUTH_200_OAUTH2_EXCHANGE_SUCCESS", "OAuth2 로그인 코드 교환에 성공했습니다."),
    OAUTH_201_SIGNUP_COMPLETE_SUCCESS(HttpStatus.CREATED, "AUTH_201_OAUTH2_SIGNUP_COMPLETE_SUCCESS", "OAuth2 회원가입 완료에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
