package com.back.devc.domain.member.member.dto;

import com.back.devc.domain.member.member.entity.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AdmMemberStatusUpdateRequest(
        @NotNull(message = "변경할 상태값은 필수입니다.")
        MemberStatus status,
        Integer days
) {}