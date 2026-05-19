package com.back.devc.domain.member.mypage.controller

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse
import com.back.devc.domain.member.mypage.dto.MyCommentResponse
import com.back.devc.domain.member.mypage.dto.MyPostResponse
import com.back.devc.domain.member.mypage.dto.MyProfileResponse
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest
import com.back.devc.domain.member.mypage.service.MypageService
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.ErrorCode
import com.back.devc.global.response.PageResponse
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.MypageSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/mypage")
class MypageController(
    private val mypageService: MypageService,
) {

    @GetMapping
    fun getMyProfile(
        @AuthenticationPrincipal principal: JwtPrincipal?,
    ): ResponseEntity<SuccessResponse<MyProfileResponse>> {
        val userId = getUserIdOrThrow(principal)

        val response = mypageService.getMyProfile(userId)
        val successCode = MypageSuccessCode.MYPAGE_200_PROFILE_FETCH

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping("/posts")
    fun getMyPosts(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ResponseEntity<SuccessResponse<PageResponse<MyPostResponse>>> {
        val userId = getUserIdOrThrow(principal)

        val response = mypageService.getMyPosts(userId, pageable)
        val successCode = MypageSuccessCode.MYPAGE_200_POSTS_FETCH

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping("/comments")
    fun getMyComments(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ResponseEntity<SuccessResponse<PageResponse<MyCommentResponse>>> {
        val userId = getUserIdOrThrow(principal)

        val response = mypageService.getMyComments(userId, pageable)
        val successCode = MypageSuccessCode.MYPAGE_200_COMMENTS_FETCH

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping("/likes")
    fun getMyLikedPosts(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ResponseEntity<SuccessResponse<PageResponse<LikedPostResponse>>> {
        val userId = getUserIdOrThrow(principal)

        val response = mypageService.getMyLikedPosts(userId, pageable)
        val successCode = MypageSuccessCode.MYPAGE_200_LIKES_FETCH

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @GetMapping("/bookmarks")
    fun getMyBookmarkedPosts(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ResponseEntity<SuccessResponse<PageResponse<BookmarkedPostResponse>>> {
        val userId = getUserIdOrThrow(principal)

        val response = mypageService.getMyBookmarkedPosts(userId, pageable)
        val successCode = MypageSuccessCode.MYPAGE_200_BOOKMARKS_FETCH

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    @PatchMapping
    fun updateMyProfile(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @RequestBody @Valid request: UpdateMyProfileRequest,
    ): ResponseEntity<SuccessResponse<MyProfileResponse>> {
        val userId = getUserIdOrThrow(principal)

        val response = mypageService.updateMyProfile(userId, request)
        val successCode = MypageSuccessCode.MYPAGE_200_PROFILE_UPDATE

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    private fun getUserIdOrThrow(principal: JwtPrincipal?): Long {
        return principal?.userId ?: throw ApiException(ErrorCode.UNAUTHORIZED)
    }
}