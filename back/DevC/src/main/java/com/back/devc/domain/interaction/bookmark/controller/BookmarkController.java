package com.back.devc.domain.interaction.bookmark.controller;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkResponse;
import com.back.devc.domain.interaction.bookmark.service.BookmarkService;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.BookmarkSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<SuccessResponse<BookmarkResponse>> createBookmark(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long postId
    ) {
        Long userId = getAuthenticatedUserId(principal);

        BookmarkResponse response = bookmarkService.createBookmark(
                new BookmarkCreateCommand(userId, postId)
        );

        BookmarkSuccessCode successCode = BookmarkSuccessCode.BOOKMARK_201_CREATE;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @DeleteMapping("/posts/{postId}/bookmarks")
    public ResponseEntity<SuccessResponse<BookmarkResponse>> cancelBookmark(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long postId
    ) {
        Long userId = getAuthenticatedUserId(principal);

        BookmarkResponse response = bookmarkService.cancelBookmark(
                new BookmarkDeleteCommand(userId, postId)
        );

        BookmarkSuccessCode successCode = BookmarkSuccessCode.BOOKMARK_200_DELETE;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }
}