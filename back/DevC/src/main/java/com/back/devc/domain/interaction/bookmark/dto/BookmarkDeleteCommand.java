package com.back.devc.domain.interaction.bookmark.dto;

public record BookmarkDeleteCommand(
        Long memberId,
        Long postId
) {}