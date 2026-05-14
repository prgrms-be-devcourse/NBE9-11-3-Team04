package com.back.devc.domain.member.member.dto;

import java.util.List;

public record PublicProfileResponse(
        Long userId,
        String nickname,
        List<PublicProfilePostResponse> posts
) {
}
