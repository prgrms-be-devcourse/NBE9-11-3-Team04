package com.back.devc.domain.post.post.controller

import com.back.devc.domain.post.post.dto.PostCreateRequest
import com.back.devc.domain.post.post.dto.PostCreateResponse
import com.back.devc.domain.post.post.dto.PostDeleteResponse
import com.back.devc.domain.post.post.dto.PostDetailResponse
import com.back.devc.domain.post.post.dto.PostListResponse
import com.back.devc.domain.post.post.dto.PostUpdateRequest
import com.back.devc.domain.post.post.dto.PostUpdateResponse
import com.back.devc.domain.post.post.service.PostService
import com.back.devc.domain.post.post.type.PostSearchType
import com.back.devc.domain.post.post.type.PostSortType
import com.back.devc.global.response.SuccessResponse
import com.back.devc.global.response.successCode.PostSuccessCode
import com.back.devc.global.security.jwt.JwtPrincipal
import com.back.devc.global.security.jwt.JwtPrincipalHelper
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService,
) {

    /**
     * 게시글 생성
     *
     * 작성자는 프론트에서 직접 넘기지 않고,
     * 현재 로그인한 사용자의 JwtPrincipal 정보로 처리
     */
    @PostMapping
    fun createPost(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @Valid @RequestBody request: PostCreateRequest,
    ): ResponseEntity<SuccessResponse<PostCreateResponse>> {
        val response = postService.write(
            JwtPrincipalHelper.getAuthenticatedUserId(principal),
            request,
        )
        val successCode = PostSuccessCode.POST_201_CREATE_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 게시글 상세 조회
     *
     * 비로그인 사용자는 null,
     * 로그인 사용자는 userId 기준으로 조회
     */
    @GetMapping("/{postId}")
    fun getPostDetail(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @PathVariable postId: Long,
    ): ResponseEntity<SuccessResponse<PostDetailResponse>> {
        val loginUserId = principal?.userId

        val response = postService.findDetailById(
            postId,
            loginUserId,
        )
        val successCode = PostSuccessCode.POST_200_DETAIL_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 게시글 목록 조회
     *
     * 카테고리, 검색어, 검색 타입, 정렬 방식 지원
     */
    @GetMapping
    fun getPosts(
        @AuthenticationPrincipal principal: JwtPrincipal?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) searchType: PostSearchType?,
        @RequestParam(defaultValue = "LATEST") sort: PostSortType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<SuccessResponse<Page<PostListResponse>>> {
        val loginUserId = principal?.userId

        val response = postService.getPosts(
            loginUserId,
            categoryId,
            keyword,
            searchType,
            sort,
            page,
            size,
        )

        val successCode = PostSuccessCode.POST_200_LIST_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 게시글 수정
     *
     * 현재 로그인한 사용자가 작성자인지 검증 후 수정
     */
    @PutMapping("/{postId}")
    fun updatePost(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable postId: Long,
        @Valid @RequestBody request: PostUpdateRequest,
    ): ResponseEntity<SuccessResponse<PostUpdateResponse>> {
        val response = postService.update(
            JwtPrincipalHelper.getAuthenticatedUserId(principal),
            postId,
            request,
        )
        val successCode = PostSuccessCode.POST_200_UPDATE_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }

    /**
     * 게시글 삭제
     *
     * 현재 로그인한 사용자 기준으로 삭제
     */
    @DeleteMapping("/{postId}")
    fun deletePost(
        @AuthenticationPrincipal principal: JwtPrincipal,
        @PathVariable postId: Long,
    ): ResponseEntity<SuccessResponse<PostDeleteResponse>> {
        val response = postService.delete(
            JwtPrincipalHelper.getAuthenticatedUserId(principal),
            postId,
        )
        val successCode = PostSuccessCode.POST_200_DELETE_SUCCESS

        return ResponseEntity
            .status(successCode.status)
            .body(SuccessResponse.of(successCode, response))
    }
}