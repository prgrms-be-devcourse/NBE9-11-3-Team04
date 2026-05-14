package com.back.devc.domain.member.mypage.dto;

public record MyProfileResponse(
        Long userId,
        String email,
        String nickname
) {
}