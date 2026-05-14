package com.back.devc.global.security.jwt;

public enum TokenValidationStatus {
    VALID,
    MISSING,
    EXPIRED,
    MALFORMED,
    UNSUPPORTED,
    INVALID_SIGNATURE,
    INVALID_TOKEN_TYPE;

    public boolean isValid() {
        return this == VALID;
    }
}
