package com.back.devc.interaction.report

import com.back.devc.global.response.SuccessResponse
import org.springframework.http.ResponseEntity
import java.util.Optional

internal fun <T : Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

internal fun <T : Any> Optional<T>.orThrow(message: String = "Expected value to be present"): T =
    orElseThrow { AssertionError(message) }

internal fun <T> ResponseEntity<SuccessResponse<T>>.successBody(): SuccessResponse<T> =
    body ?: throw AssertionError("Expected response body")
