package com.back.devc.global.response

import org.springframework.http.HttpStatus

interface SuccessCodeSpec {
    val status: HttpStatus
    val code: String
    val message: String
}