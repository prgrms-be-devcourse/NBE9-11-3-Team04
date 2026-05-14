package com.back.devc.domain.member.mypage.dto;

import java.util.List;

public record MyCommentsResponse(
        List<MyCommentResponse> comments
) {
}