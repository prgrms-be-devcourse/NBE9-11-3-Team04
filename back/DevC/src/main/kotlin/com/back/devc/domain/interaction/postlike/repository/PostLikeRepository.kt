package com.back.devc.domain.interaction.postLike.repository

import com.back.devc.domain.interaction.postLike.entity.PostLike
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.post.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostLikeRepository : JpaRepository<PostLike, Long> {

    /**
     * 특정 회원이 특정 게시글에 좋아요를 눌렀는지 확인
     */
    fun existsByMemberAndPost(
        member: Member,
        post: Post,
    ): Boolean

    /**
     * 특정 회원의 특정 게시글 좋아요 엔티티 조회
     */
    fun findByMemberAndPost(
        member: Member,
        post: Post,
    ): PostLike?

    /**
     * 특정 회원이 좋아요한 게시글 목록 조회
     */
    fun findAllByMember(
        member: Member,
    ): List<PostLike>

    /**
     * 삭제되지 않은 게시글에 대한 좋아요 목록만 조회
     */
    fun findAllByMemberAndPost_IsDeletedFalse(
        member: Member,
    ): List<PostLike>

    /**
     * 삭제되지 않은 게시글에 대한 좋아요 목록 페이징 조회
     */
    fun findAllByMemberAndPost_IsDeletedFalse(
        member: Member,
        pageable: Pageable,
    ): Page<PostLike>

    /**
     * 특정 게시글에 연결된 좋아요 전체 삭제
     */
    fun deleteByPost_PostId(
        postId: Long,
    )

    /**
     * userId, postId 기반 존재 여부 확인
     */
    fun existsByMember_UserIdAndPost_PostId(
        userId: Long,
        postId: Long,
    ): Boolean

    /**
     * 북마크/좋아요 목록 조회 최적화용
     *
     * 특정 사용자가 좋아요한 게시글 ID만 한 번에 조회한다.
     * 목록 DTO 매핑 시 exists 쿼리가 N번 나가는 문제를 방지한다.
     */
    @Query(
        """
            select pl.post.postId
            from PostLike pl
            where pl.member.userId = :userId
            and pl.post.postId in :postIds
        """
    )
    fun findLikedPostIdsByUserIdAndPostIds(
        @Param("userId") userId: Long,
        @Param("postIds") postIds: Collection<Long>,
    ): List<Long>

    /**
     * 좋아요 생성
     *
     * MySQL / MariaDB 기준:
     * - 처음 좋아요면 1 반환
     * - 이미 좋아요가 있으면 0 반환
     */
    @Modifying
    @Query(
        value = """
            insert ignore into post_likes (user_id, post_id, created_at)
            values (:userId, :postId, now())
        """,
        nativeQuery = true,
    )
    fun insertIgnore(
        @Param("userId") userId: Long,
        @Param("postId") postId: Long,
    ): Int

    /**
     * 좋아요 취소
     *
     * - 실제 삭제되면 1 반환
     * - 이미 취소된 상태면 0 반환
     */
    @Modifying
    @Query(
        """
            delete from PostLike pl
            where pl.member.userId = :userId
            and pl.post.postId = :postId
        """
    )
    fun deleteByUserIdAndPostId(
        @Param("userId") userId: Long,
        @Param("postId") postId: Long,
    ): Int

    /**
     * 동시성 테스트 또는 검증용
     */
    fun countByPost_PostId(
        postId: Long,
    ): Long
}