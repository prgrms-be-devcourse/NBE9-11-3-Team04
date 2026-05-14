package com.back.devc.global.exception;

public class ApiException extends RuntimeException {

    private final ErrorCodeSpec errorCode;

    public ApiException(ErrorCodeSpec errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCodeSpec getErrorCode() {
        return errorCode;
    }
}
