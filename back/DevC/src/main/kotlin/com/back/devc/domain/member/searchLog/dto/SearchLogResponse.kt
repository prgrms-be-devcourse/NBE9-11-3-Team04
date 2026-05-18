package com.back.devc.domain.member.searchLog.dto

import java.time.LocalDateTime

data class SearchLogResponse(
    val searchLogId: Long,
    val keyword: String,
    val searchedAt: LocalDateTime,
)