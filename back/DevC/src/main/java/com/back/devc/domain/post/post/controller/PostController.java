package com.back.devc.domain.post.post.controller;

import com.back.devc.domain.post.post.dto.*;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.service.PostService;
import com.back.devc.domain.post.post.type.PostSearchType;
import com.back.devc.domain.post.post.type.PostSortType;
import com.back.devc.global.response.successCode.PostSuccessCode;
import com.back.devc.global.security.jwt.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.back.devc.global.response.SuccessCode;
import com.back.devc.global.response.SuccessResponse;

import static com.back.devc.global.security.jwt.JwtPrincipalHelper.getAuthenticatedUserId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // 게시글 생성
    @PostMapping
    public ResponseEntity<SuccessResponse<PostCreateResponse>> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody @Valid PostCreateRequest request
    ) {
        Long userId = getAuthenticatedUserId(principal);

        PostCreateResponse response = postService.write(userId, request);

        PostSuccessCode successCode = PostSuccessCode.POST_201_CREATE_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    //게시글 상세조회
    @GetMapping("/{postid}")
    public ResponseEntity<SuccessResponse<PostDetailResponse>> detail(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long postid
    ) {
        Long loginUserId = principal != null ? principal.userId() : null;

        PostDetailResponse response = postService.findDetailById(postid, loginUserId);
        PostSuccessCode successCode = PostSuccessCode.POST_200_DETAIL_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));

    }

    //게시글 목록조회
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<PostListResponse>>> list(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PostSearchType searchType,
            @RequestParam(defaultValue = "LATEST") PostSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long loginUserId = principal != null ? getAuthenticatedUserId(principal) : null;

        Page<PostListResponse> response = postService.getPosts(
                loginUserId,
                categoryId,
                keyword,
                searchType,
                sort,
                page,
                size
        );

        PostSuccessCode successCode = PostSuccessCode.POST_200_LIST_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));

    }

    //게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<SuccessResponse<PostUpdateResponse>> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequest request
    ) {
        Long userId = getAuthenticatedUserId(principal);

        PostUpdateResponse response = postService.update(userId, postId, request);

        PostSuccessCode successCode = PostSuccessCode.POST_200_UPDATE_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }

    //게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<SuccessResponse<PostDeleteResponse>> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long postId
    ) {
        Long userId = getAuthenticatedUserId(principal);

        PostDeleteResponse response = postService.delete(userId, postId);

        PostSuccessCode successCode = PostSuccessCode.POST_200_DELETE_SUCCESS;

        return ResponseEntity
                .status(successCode.getStatus())
                .body(SuccessResponse.of(successCode, response));
    }
}