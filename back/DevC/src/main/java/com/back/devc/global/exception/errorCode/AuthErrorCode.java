package com.back.devc.global.exception.errorCode;

import com.back.devc.global.exception.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCodeSpec {
    BAD_REQUEST(HttpStatus.BAD_REQUEST,
            "COMMON_400",
            "잘못된 요청입니다."
    ),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "COMMON_401",
            "인증이 필요합니다."
    ),
    ALREADY_DELETED(HttpStatus.GONE,
            "COMMON_410",
            "이미 삭제된 리소스입니다."
    ),

    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND,
            "AUTH_404_EMAIL_NOT_FOUND",
            "존재하지 않는 이메일입니다."
    ),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED,
            "AUTH_401_PASSWORD_MISMATCH",
            "비밀번호가 일치하지 않습니다."
    ),
    MEMBER_BLACKLISTED(HttpStatus.FORBIDDEN,
            "AUTH_403_MEMBER_BLACKLISTED",
            "이용할 수 없는 계정입니다."
    ),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT,
            "AUTH_409_EMAIL",
            "이미 사용 중인 이메일입니다."
    ),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT,
            "AUTH_409_NICKNAME",
            "이미 사용 중인 닉네임입니다."
    ),

    OAUTH2_PENDING_SIGNUP_REQUIRED(HttpStatus.UNAUTHORIZED,
            "AUTH_401_OAUTH2_PENDING_SIGNUP_REQUIRED",
            "OAuth 회원가입 정보가 없습니다. 다시 로그인해주세요."
    ),
    OAUTH2_PENDING_SIGNUP_EXPIRED(HttpStatus.UNAUTHORIZED,
            "AUTH_401_OAUTH2_PENDING_SIGNUP_EXPIRED",
            "OAuth 회원가입 세션이 만료되었습니다. 다시 로그인해주세요."
    ),
    OAUTH2_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST,
            "AUTH_400_OAUTH2_UNSUPPORTED_PROVIDER",
            "지원하지 않는 OAuth 제공자입니다."
    ),
    OAUTH2_PROVIDER_USER_ID_MISSING(HttpStatus.BAD_REQUEST,
            "AUTH_400_OAUTH2_PROVIDER_USER_ID_MISSING",
            "OAuth 제공자 사용자 식별값을 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;


}
