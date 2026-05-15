package com.back.devc.domain.auth.dto.oauth

import java.io.Serializable

data class OAuthPendingSignup(
    val provider: String,
    val providerUserId: String,
    val emailFromProvider: String,
    val loginFromProvider: String
) : Serializable
