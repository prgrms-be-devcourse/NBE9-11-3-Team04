package com.back.devc.domain.interaction.postLike.controller;

import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery;
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand;
import com.back.devc.domain.interaction.postLike.dto.PostLikeResponse;
import com.back.devc.domain.interaction.postLike.service.PostLikeService;
import com.back.devc.global.response.SuccessResponse;
import com.back.devc.global.response.successCode.PostLikeSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<SuccessResponse<PostLikeResponse>> createLike(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long postId
    ) {
        PostLikeCommand command = new PostLikeCommand(
                getAuthenticatedUserId(principal),
                postId
        );

        PostLikeResponse response = postLikeService.createLike(command);
        PostLikeSuccessCode successCode = PostLikeSuccessCode.POST_LIKE_CREATED;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ResponseEntity<SuccessResponse<PostLikeResponse>> cancelLike(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long postId
    ) {
        PostLikeCommand command = new PostLikeCommand(
                getAuthenticatedUserId(principal),
                postId
        );

        PostLikeResponse response = postLikeService.cancelLike(command);
        PostLikeSuccessCode successCode = PostLikeSuccessCode.POST_LIKE_CANCELED;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    @GetMapping("/users/me/likes")
    public ResponseEntity<SuccessResponse<List<LikedPostResponse>>> getLikedPosts(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        LikedPostsQuery query = new LikedPostsQuery(
                getAuthenticatedUserId(principal)
        );

        List<LikedPostResponse> response = postLikeService.getLikedPosts(query);
        PostLikeSuccessCode successCode = PostLikeSuccessCode.LIKED_POSTS_FETCHED;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }
}