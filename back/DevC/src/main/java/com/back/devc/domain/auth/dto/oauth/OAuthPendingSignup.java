package com.back.devc.domain.auth.dto.oauth;

import java.io.Serializable;

public record OAuthPendingSignup(
        String provider,
        String providerUserId,
        String emailFromProvider,
        String loginFromProvider
) implements Serializable {
}
