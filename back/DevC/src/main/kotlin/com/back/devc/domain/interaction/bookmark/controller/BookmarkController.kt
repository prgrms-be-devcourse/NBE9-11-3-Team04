package com.back.devc.domain.interaction.bookmark.controller

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkResponse
import com.back.devc.domain.interaction.bookmark.service.BookmarkService
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.BookmarkSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class BookmarkController(
    private val bookmarkService: BookmarkService,
) {

    @PostMapping("/posts/{postId}/bookmarks")
    fun createBookmark(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable postId: Long,
    ): ResponseEntity<SuccessResponse<BookmarkResponse>> {
        val userId = getAuthenticatedUserId(principal)

        val response = bookmarkService.createBookmark(
            BookmarkCreateCommand(
                memberId = userId,
                postId = postId,
            )
        )

        val successCode = BookmarkSuccessCode.BOOKMARK_201_CREATE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @DeleteMapping("/posts/{postId}/bookmarks")
    fun cancelBookmark(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable postId: Long,
    ): ResponseEntity<SuccessResponse<BookmarkResponse>> {
        val userId = getAuthenticatedUserId(principal)

        val response = bookmarkService.cancelBookmark(
            BookmarkDeleteCommand(
                memberId = userId,
                postId = postId,
            )
        )

        val successCode = BookmarkSuccessCode.BOOKMARK_200_DELETE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }
}