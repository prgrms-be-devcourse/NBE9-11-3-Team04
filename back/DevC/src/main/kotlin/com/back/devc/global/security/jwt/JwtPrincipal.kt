package com.back.devc.global.security.jwt

data class JwtPrincipal(
    val userId: Long,
    val email: String,
    val role: String
) {
    // Java record accessor 호환을 위해 임시 유지
    fun userId(): Long = userId
    fun email(): String = email
    fun role(): String = role
}