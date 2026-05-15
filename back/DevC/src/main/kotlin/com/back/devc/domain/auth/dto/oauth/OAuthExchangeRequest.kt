package com.back.devc.domain.auth.dto.oauth

import jakarta.validation.constraints.NotBlank

data class OAuthExchangeRequest(
    @field:NotBlank(message = "code는 필수입니다.")
    val code: String
)
