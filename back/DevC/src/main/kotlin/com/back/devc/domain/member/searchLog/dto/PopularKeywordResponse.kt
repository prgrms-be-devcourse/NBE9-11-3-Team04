package com.back.devc.domain.member.searchLog.dto

data class PopularKeywordResponse(
    val keyword: String,
    val count: Long,
)