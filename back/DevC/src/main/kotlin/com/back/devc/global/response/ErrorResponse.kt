package com.back.devc.global.response

import com.back.devc.global.exception.ErrorCodeSpec
import java.time.LocalDateTime

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime,
    val validation: Map<String, String>
) {

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