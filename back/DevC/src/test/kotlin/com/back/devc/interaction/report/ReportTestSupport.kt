package com.back.devc.interaction.report

import com.back.devc.global.response.SuccessResponse
import org.springframework.http.ResponseEntity

internal fun Any.setPrivateField(fieldName: String, value: Any?) {
    val field = javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(this, value)
}

internal fun <T> ResponseEntity<SuccessResponse<T>>.successBody(): SuccessResponse<T> =
    body ?: throw AssertionError("Expected response body")
