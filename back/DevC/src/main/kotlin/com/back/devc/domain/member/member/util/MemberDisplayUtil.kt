package com.back.devc.domain.member.member.util

import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.entity.MemberStatus

object MemberDisplayUtil {

    @JvmStatic
    fun getDisplayName(member: Member?): String {
        if (member == null) {
            return ""
        }

        if (member.status == MemberStatus.WITHDRAWN) {
            return "탈퇴한 회원"
        }

        return member.nickname
    }
}
