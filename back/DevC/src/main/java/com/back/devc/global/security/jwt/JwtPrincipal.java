package com.back.devc.global.security.jwt;

public record JwtPrincipal(
        Long userId,
        String email,
        String role
) {
}
