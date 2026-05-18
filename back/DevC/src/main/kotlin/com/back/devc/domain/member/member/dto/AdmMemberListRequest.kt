package com.back.devc.domain.member.member.dto

import com.back.devc.domain.member.member.entity.MemberStatus

data class AdmMemberListRequest(
    val page: Int,
    val size: Int,
    val keyword: String?,
    val status: MemberStatus?,
)

