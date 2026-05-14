package com.back.devc.global.response;

import org.springframework.http.HttpStatus;

public interface SuccessCodeSpec {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
