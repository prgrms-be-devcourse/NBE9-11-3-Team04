package com.back.devc.domain.member.mypage.dto

data class MyProfileResponse(
    val userId: Long,
    val email: String,
    val nickname: String,
)