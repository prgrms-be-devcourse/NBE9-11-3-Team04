package com.back.devc

import java.util.Optional as RepositoryResult

internal fun <T : Any> T?.toRepositoryResult(): RepositoryResult<T> =
    this?.let { RepositoryResult.of(it) } ?: RepositoryResult.empty()

internal fun <T : Any> RepositoryResult<T>.toNullable(): T? =
    if (isPresent) get() else null
