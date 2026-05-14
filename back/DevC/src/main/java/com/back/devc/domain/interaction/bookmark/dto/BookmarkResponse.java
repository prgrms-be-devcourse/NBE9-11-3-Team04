package com.back.devc.domain.interaction.bookmark.dto;

public record BookmarkResponse(
        long postId,
        boolean bookmarked
) {
}