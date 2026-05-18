package com.back.devc.global.exception

class ApiException(
    val errorCode: ErrorCodeSpec
) : RuntimeException(errorCode.message)