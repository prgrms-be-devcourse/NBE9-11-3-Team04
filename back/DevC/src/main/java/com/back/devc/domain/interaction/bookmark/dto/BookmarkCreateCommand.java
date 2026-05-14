package com.back.devc.domain.interaction.bookmark.dto;

public record BookmarkCreateCommand(
        Long memberId,
        Long postId
) {}