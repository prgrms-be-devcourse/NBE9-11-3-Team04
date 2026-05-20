package com.back.devc.domain.auth.dto.oauth

data class OAuth2MeResponse(
    val authenticated: Boolean,
    val name: String?,
    val authorities: List<String>,
    val attributes: Map<String, Any>
)