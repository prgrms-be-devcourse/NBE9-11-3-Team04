package com.back.devc.domain.post.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하입니다.")
        String title,
        @NotBlank(message = "내용은 필수입니다.")
        String content,
        @NotNull(message = "카테고리는 필수입니다.")
        Long categoryId
) {}