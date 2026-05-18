package com.back.devc.domain.post.post.repository

import com.back.devc.domain.member.member.dto.CountResultDto
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.post.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface PostRepository : JpaRepository<Post, Long> {

    fun findByIsDeletedFalse(pageable: Pageable): Page<Post>

    fun findByCategoryCategoryIdAndIsDeletedFalse(
        categoryId: Long,
        pageable: Pageable
    ): Page<Post>

    fun findByTitleContainingAndIsDeletedFalse(
        title: String,
        pageable: Pageable
    ): Page<Post>

    fun findByContentContainingAndIsDeletedFalse(
        content: String,
        pageable: Pageable
    ): Page<Post>

    @Query(
        """
        SELECT p FROM Post p
        WHERE p.isDeleted = false
          AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)
        """
    )
    fun searchByKeyword(
        @Param("kw") kw: String,
        pageable: Pageable
    ): Page<Post>

    @Query(
        """
        SELECT p FROM Post p
        WHERE p.isDeleted = false
          AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
          AND (p.title LIKE %:kw% OR p.content LIKE %:kw%)
        """
    )
    fun searchPosts(
        @Param("categoryId") categoryId: Long?,
        @Param("kw") kw: String,
        pageable: Pageable
    ): Page<Post>

    fun findByCategoryCategoryIdAndTitleContainingAndIsDeletedFalse(
        categoryId: Long,
        title: String,
        pageable: Pageable
    ): Page<Post>

    fun findByCategoryCategoryIdAndContentContainingAndIsDeletedFalse(
        categoryId: Long,
        content: String,
        pageable: Pageable
    ): Page<Post>

    fun findAllByOrderByCreatedAtDesc(): List<Post>

    fun findAllByMemberAndIsDeletedFalseOrderByCreatedAtDesc(
        member: Member,
        pageable: Pageable
    ): Page<Post>

    fun findTop20ByMemberAndIsDeletedFalseOrderByCreatedAtDesc(
        member: Member
    ): List<Post>

    fun findByPostIdAndIsDeletedFalse(id: Long): Optional<Post>

    // Batch IN 신고 조회용
    fun findAllByPostIdIn(postIds: List<Long>): List<Post>

    // Batch IN 유저 목록 조회용
    @Query(
        """
        SELECT new com.back.devc.domain.member.member.dto.CountResultDto(
            p.member.userId,
            COUNT(p)
        )
        FROM Post p
        WHERE p.member.userId IN :userIds
        GROUP BY p.member.userId
        """
    )
    fun countPostsByUserIds(
        @Param("userIds") userIds: List<Long>
    ): List<CountResultDto>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE Post p
        SET p.likeCount = p.likeCount + 1
        WHERE p.postId = :postId
          AND p.isDeleted = false
        """
    )
    fun increaseLikeCount(
        @Param("postId") postId: Long
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE Post p
        SET p.likeCount = p.likeCount - 1
        WHERE p.postId = :postId
          AND p.isDeleted = false
          AND p.likeCount > 0
        """
    )
    fun decreaseLikeCount(
        @Param("postId") postId: Long
    ): Int

    @Query(
        """
        SELECT p.likeCount
        FROM Post p
        WHERE p.postId = :postId
          AND p.isDeleted = false
        """
    )
    fun findLikeCountByPostId(
        @Param("postId") postId: Long
    ): Int
}