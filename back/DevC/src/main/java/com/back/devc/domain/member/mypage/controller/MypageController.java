package com.back.devc.domain.member.mypage.controller;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.member.mypage.dto.MyCommentResponse;
import com.back.devc.domain.member.mypage.dto.MyPostResponse;
import com.back.devc.domain.member.mypage.dto.MyProfileResponse;
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest;
import com.back.devc.domain.member.mypage.service.MypageService;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.ErrorCode;
import com.back.devc.global.response.PageResponse;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.MypageSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping
    public ResponseEntity<SuccessResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        validatePrincipal(principal);

        MyProfileResponse response = mypageService.getMyProfile(principal.userId());
        MypageSuccessCode successCode = MypageSuccessCode.MYPAGE_200_PROFILE_FETCH;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @GetMapping("/posts")
    public ResponseEntity<SuccessResponse<PageResponse<MyPostResponse>>> getMyPosts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        validatePrincipal(principal);

        PageResponse<MyPostResponse> response =
                mypageService.getMyPosts(principal.userId(), pageable);

        MypageSuccessCode successCode = MypageSuccessCode.MYPAGE_200_POSTS_FETCH;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @GetMapping("/comments")
    public ResponseEntity<SuccessResponse<PageResponse<MyCommentResponse>>> getMyComments(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        validatePrincipal(principal);

        PageResponse<MyCommentResponse> response =
                mypageService.getMyComments(principal.userId(), pageable);

        MypageSuccessCode successCode = MypageSuccessCode.MYPAGE_200_COMMENTS_FETCH;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @GetMapping("/likes")
    public ResponseEntity<SuccessResponse<PageResponse<LikedPostResponse>>> getMyLikedPosts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        validatePrincipal(principal);

        PageResponse<LikedPostResponse> response =
                mypageService.getMyLikedPosts(principal.userId(), pageable);

        MypageSuccessCode successCode = MypageSuccessCode.MYPAGE_200_LIKES_FETCH;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<SuccessResponse<PageResponse<BookmarkedPostResponse>>> getMyBookmarkedPosts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        validatePrincipal(principal);

        PageResponse<BookmarkedPostResponse> response =
                mypageService.getMyBookmarkedPosts(principal.userId(), pageable);

        MypageSuccessCode successCode = MypageSuccessCode.MYPAGE_200_BOOKMARKS_FETCH;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @PatchMapping
    public ResponseEntity<SuccessResponse<MyProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody @Valid UpdateMyProfileRequest request
    ) {
        validatePrincipal(principal);

        MyProfileResponse response = mypageService.updateMyProfile(principal.userId(), request);
        MypageSuccessCode successCode = MypageSuccessCode.MYPAGE_200_PROFILE_UPDATE;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    private void validatePrincipal(JwtPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }
}