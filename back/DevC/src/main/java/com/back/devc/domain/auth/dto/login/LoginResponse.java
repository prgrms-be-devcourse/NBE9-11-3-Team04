package com.back.devc.domain.auth.dto.login;

import com.back.devc.domain.member.member.entity.MemberRole;
import com.back.devc.domain.member.member.entity.MemberStatus;

public record LoginResponse(
        Long userId,
        String email,
        String nickname,
        MemberRole role,
        MemberStatus status,
        String accessToken
) {
}
