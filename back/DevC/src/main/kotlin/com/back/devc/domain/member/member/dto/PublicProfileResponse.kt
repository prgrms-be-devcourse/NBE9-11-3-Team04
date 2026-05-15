package com.back.devc.domain.member.member.dto

data class PublicProfileResponse(
    val userId: Long,
    val nickname: String,
    val posts: List<PublicProfilePostResponse>
)
