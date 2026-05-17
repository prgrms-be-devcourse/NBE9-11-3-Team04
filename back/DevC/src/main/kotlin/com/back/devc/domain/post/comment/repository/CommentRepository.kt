package com.back.devc.domain.post.comment.repository

import com.back.devc.domain.member.member.dto.CountResultDto
import com.back.devc.domain.member.mypage.dto.MyCommentResponse
import com.back.devc.domain.post.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<Comment, Long> {
    // 게시글 기준 댓글 조회
    fun findByPostIdOrderByCreatedAtAsc(postId: Long): List<Comment>

    // 게시글 상세 페이지 댓글 페이징 조회
    fun findByPostIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(
        postId: Long,
        pageable: Pageable,
    ): Page<Comment>

    // 게시글 삭제 시 해당 게시글의 삭제되지 않은 댓글/대댓글 조회
    fun findByPostIdAndIsDeletedFalse(postId: Long): List<Comment>

    // 내가 쓴 댓글 조회
    fun findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId: Long): List<Comment>

    fun countByUserIdAndIsDeletedFalse(userId: Long): Long

    fun countByPostIdAndIsDeletedFalse(postId: Long): Long

    @Query(
        """
        SELECT new com.back.devc.domain.member.mypage.dto.MyCommentResponse(
            c.id,
            c.postId,
            p.title,
            c.content,
            c.createdAt
        )
        FROM Comment c
        JOIN Post p ON c.postId = p.postId
        WHERE c.userId = :userId
          AND c.isDeleted = false
          AND p.isDeleted = false
        ORDER BY c.createdAt DESC
        """
    )
    fun findMyComments(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<MyCommentResponse>

    // Batch IN 신고 조회용
    fun findAllByIdIn(ids: List<Long>): List<Comment>

    // Batch IN 유저 목록 조회용
    @Query(
        """
        SELECT new com.back.devc.domain.member.member.dto.CountResultDto(c.userId, COUNT(c))
        FROM Comment c
        WHERE c.userId IN :userIds
          AND c.isDeleted = false
        GROUP BY c.userId
        """
    )
    fun countCommentsByUserIds(@Param("userIds") userIds: List<Long>): List<CountResultDto>
}