package com.back.devc.domain.auth.dto.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthSignupCompleteRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하입니다.")
        String nickname
) {
}
