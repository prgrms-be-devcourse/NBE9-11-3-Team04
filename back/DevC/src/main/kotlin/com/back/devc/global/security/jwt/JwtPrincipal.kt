package com.back.devc.global.security.jwt

data class JwtPrincipal(
    val userId: Long,
    val email: String,
    val role: String
)