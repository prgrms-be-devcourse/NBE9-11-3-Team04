package com.back.devc.domain.post.post.controller

import com.back.devc.domain.post.post.dto.AdminPostDetailResponse
import com.back.devc.domain.post.post.dto.AdminPostListResponse
import com.back.devc.domain.post.post.service.AdminPostService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
class AdminPostController(
    private val adminPostService: AdminPostService,
) {

    /**
     * 관리자 게시글 전체 조회
     *
     * 삭제 여부 포함 전체 게시글 관리용 조회
     */
    @GetMapping
    fun getAllPosts(): ResponseEntity<List<AdminPostListResponse>> {
        val response = adminPostService.findAll()
            .map { post ->
                AdminPostListResponse.Companion.from(post)
            }

        return ResponseEntity.ok(response)
    }

    /**
     * 관리자 게시글 상세 조회
     *
     * 특정 게시글 상세 정보 조회
     */
    @GetMapping("/{postId}")
    fun getPostDetail(
        @PathVariable postId: Long,
    ): ResponseEntity<AdminPostDetailResponse> {
        val response = adminPostService.findDetailById(postId)

        return ResponseEntity.ok(response)
    }
}