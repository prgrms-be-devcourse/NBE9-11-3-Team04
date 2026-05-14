package com.back.devc.domain.member.member.util;

import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.entity.MemberStatus;

public final class MemberDisplayUtil {

    private MemberDisplayUtil() {
    }

    public static String getDisplayName(Member member) {
        if (member == null) {
            return null;
        }

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            return "탈퇴한 회원";
        }

        return member.getNickname();
    }
}