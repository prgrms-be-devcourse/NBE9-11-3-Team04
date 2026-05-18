package com.back.devc.global.response

import com.back.devc.global.exception.ErrorCodeSpec
import java.time.LocalDateTime

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime,
    val validation: Map<String, String>
) {
    // Java record accessor 호환을 위해 임시 유지
    fun code(): String = code
    fun message(): String = message
    fun timestamp(): LocalDateTime = timestamp
    fun validation(): Map<String, String> = validation

    companion object {
        @JvmStatic
        fun of(errorCode: ErrorCodeSpec): ErrorResponse {
            return ErrorResponse(
                code = errorCode.code,
                message = errorCode.message,
                timestamp = LocalDateTime.now(),
                validation = emptyMap()
            )
        }

        @JvmStatic
        fun of(
            errorCode: ErrorCodeSpec,
            validation: Map<String, String>
        ): ErrorResponse {
            return ErrorResponse(
                code = errorCode.code,
                message = errorCode.message,
                timestamp = LocalDateTime.now(),
                validation = validation
            )
        }
    }
}