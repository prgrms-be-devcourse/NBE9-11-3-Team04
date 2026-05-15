package com.back.devc.domain.auth.dto.logout

data class LogoutResponse(
    val message: String
) {
    companion object {
        @JvmStatic
        fun success(): LogoutResponse =
            LogoutResponse("로그아웃이 완료되었습니다.")
    }
}