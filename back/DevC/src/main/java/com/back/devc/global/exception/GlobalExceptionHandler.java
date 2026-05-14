package com.back.devc.global.exception;

import com.back.devc.global.exception.errorCode.AuthErrorCode;
import com.back.devc.global.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        ErrorCodeSpec errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> validation = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null ? "잘못된 값입니다." : fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        return ResponseEntity
                .status(AuthErrorCode.BAD_REQUEST.getStatus())
                .body(ErrorResponse.of(AuthErrorCode.BAD_REQUEST, validation));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationCredentialsNotFoundException(
            AuthenticationCredentialsNotFoundException e
    ) {
        return ResponseEntity
                .status(AuthErrorCode.UNAUTHORIZED.getStatus())
                .body(ErrorResponse.of(AuthErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(jakarta.persistence.EntityNotFoundException e) {
        return ResponseEntity.status(404)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
    }
}
