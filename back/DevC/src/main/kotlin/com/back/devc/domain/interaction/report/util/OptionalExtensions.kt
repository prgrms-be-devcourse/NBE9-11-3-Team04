package com.back.devc.domain.interaction.report.util

import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCodeSpec
import java.util.Optional

internal fun <T : Any> Optional<T>.getOrNull(): T? =
    orElse(null)

internal fun <T : Any> Optional<T>.getOrThrow(errorCode: ErrorCodeSpec): T =
    getOrNull() ?: throw ApiException(errorCode)
