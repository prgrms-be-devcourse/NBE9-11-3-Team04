package com.back.devc.domain.auth.dto.signup;

import com.back.devc.domain.member.member.entity.MemberRole;
import com.back.devc.domain.member.member.entity.MemberStatus;

public record SignUpResponse(
        Long userId,
        String email,
        String nickname,
        MemberRole role,
        MemberStatus status
) {
}
