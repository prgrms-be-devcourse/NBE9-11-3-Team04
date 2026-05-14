package com.back.devc.domain.member.member.dto;

import com.back.devc.domain.member.member.entity.MemberStatus;
import lombok.Builder;

@Builder
public record AdmMemberListRequest(
        int page,
        int size,
        String keyword,
        MemberStatus status
) {
}