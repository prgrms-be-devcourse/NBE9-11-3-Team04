package com.back.devc.domain.member.mypage.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateMyProfileRequest(
    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
    val nickname: String,
)