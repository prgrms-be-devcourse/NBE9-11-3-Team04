package com.back.devc.domain.post.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
        String content
) {

}