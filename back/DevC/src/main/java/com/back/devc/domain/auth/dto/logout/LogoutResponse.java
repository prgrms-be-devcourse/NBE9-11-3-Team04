package com.back.devc.domain.auth.dto.logout;

public record LogoutResponse(
        String message
) {
    public static LogoutResponse success() {
        return new LogoutResponse("로그아웃이 완료되었습니다.");
    }
}
