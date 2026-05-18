package com.back.devc.domain.post.post.service

import com.back.devc.domain.post.post.dto.AdminPostDetailResponse
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminPostService(
    private val postRepository: PostRepository
) {

    // 전체 조회 (최신순 - 관리자용 isDeleted 포함)
    @Transactional(readOnly = true)
    fun findAll(): List<Post> {
        return postRepository.findAllByOrderByCreatedAtDesc()
    }

    @Transactional(readOnly = true)
    fun findDetailById(postId: Long): AdminPostDetailResponse {
        val post = postRepository.findById(postId)
            .orElseThrow {
                EntityNotFoundException("게시글을 찾을 수 없습니다. id=$postId")
            }

        return AdminPostDetailResponse.Companion.from(post)
    }
}