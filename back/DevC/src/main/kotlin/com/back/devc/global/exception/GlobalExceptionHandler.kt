package com.back.devc.global.exception

import com.back.devc.global.exception.errorCode.AuthErrorCode
import com.back.devc.global.response.ErrorResponse
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException): ResponseEntity<ErrorResponse> {
        val errorCode = e.errorCode

        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(errorCode))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException
    ): ResponseEntity<ErrorResponse> {
        val validation = e.bindingResult
            .fieldErrors
            .associate { fieldError: FieldError ->
                fieldError.field to (fieldError.defaultMessage ?: "잘못된 값입니다.")
            }

        return ResponseEntity
            .status(AuthErrorCode.BAD_REQUEST.status)
            .body(ErrorResponse.of(AuthErrorCode.BAD_REQUEST, validation))
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException::class)
    fun handleAuthenticationCredentialsNotFoundException(
        e: AuthenticationCredentialsNotFoundException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(AuthErrorCode.UNAUTHORIZED.status)
            .body(ErrorResponse.of(AuthErrorCode.UNAUTHORIZED))
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(e: EntityNotFoundException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity
            .status(404)
            .body(mapOf("message" to e.message))
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBadRequest(e: RuntimeException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity
            .badRequest()
            .body(mapOf("message" to e.message))
    }
}