package com.back.devc.domain.auth.dto.oauth;

import jakarta.validation.constraints.NotBlank;

public record OAuthExchangeRequest(
        @NotBlank(message = "code는 필수입니다.")
        String code
) {
}
