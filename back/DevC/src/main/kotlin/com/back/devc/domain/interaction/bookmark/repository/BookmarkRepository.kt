package com.back.devc.domain.interaction.bookmark.repository

import com.back.devc.domain.interaction.bookmark.entity.Bookmark
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.post.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BookmarkRepository : JpaRepository<Bookmark, Long> {

    fun existsByMemberAndPost(
        member: Member,
        post: Post,
    ): Boolean

    fun findByMemberAndPost(
        member: Member,
        post: Post,
    ): Bookmark?

    fun findAllByMember(
        member: Member,
    ): List<Bookmark>

    fun findAllByMemberAndPost_IsDeletedFalse(
        member: Member,
    ): List<Bookmark>

    fun findAllByMemberAndPost_IsDeletedFalse(
        member: Member,
        pageable: Pageable,
    ): Page<Bookmark>

    /**
     * 북마크 목록 조회 - List
     *
     * Bookmark -> Post -> Member, Category 를 fetch join 해서
     * 목록 DTO 매핑 시 N+1 방지
     */
    @Query(
        """
            select b
            from Bookmark b
            join fetch b.post p
            join fetch p.member m
            join fetch p.category c
            where b.member.userId = :userId
            and p.isDeleted = false
            order by b.createdAt desc
        """
    )
    fun findAllWithPostMemberCategoryByUserId(
        @Param("userId") userId: Long,
    ): List<Bookmark>

    /**
     * 북마크 목록 조회 - Paging
     *
     * Bookmark -> Post -> Member, Category 를 fetch join 해서
     * 목록 DTO 매핑 시 N+1 방지
     *
     * ManyToOne fetch join 이므로 paging 적용 가능.
     * 단, countQuery 는 별도 지정.
     */
    @Query(
        value = """
            select b
            from Bookmark b
            join fetch b.post p
            join fetch p.member m
            join fetch p.category c
            where b.member.userId = :userId
            and p.isDeleted = false
        """,
        countQuery = """
            select count(b)
            from Bookmark b
            join b.post p
            where b.member.userId = :userId
            and p.isDeleted = false
        """
    )
    fun findPageWithPostMemberCategoryByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<Bookmark>

    @Modifying
    @Query(
        """
            delete from Bookmark b
            where b.post.postId = :postId
        """
    )
    fun deleteByPost_PostId(
        @Param("postId") postId: Long,
    ): Int

    /**
     * 현재 로그인한 사용자가 특정 게시글을 북마크했는지 확인
     *
     * 게시글 상세조회 응답에서 bookmarked 값을 내려주기 위해 사용
     */
    fun existsByMember_UserIdAndPost_PostId(
        userId: Long,
        postId: Long,
    ): Boolean

    /**
     * 삭제되지 않은 게시글에 대해서만 북마크 여부 확인
     */
    fun existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(
        userId: Long,
        postId: Long,
    ): Boolean

    /**
     * 특정 게시글의 전체 북마크 수를 조회
     *
     * 게시글 상세 페이지에서 북마크 수를 바로 표시할 수 있도록
     * 상세조회 응답에 bookmarkCount 를 포함할 때 사용
     */
    fun countByPost_PostId(
        postId: Long,
    ): Long

    /**
     * 북마크 생성
     *
     * MySQL / MariaDB 기준:
     * - 처음 북마크면 1 반환
     * - 이미 북마크가 있으면 0 반환
     */
    @Modifying
    @Query(
        value = """
            insert ignore into bookmarks (user_id, post_id, created_at)
            values (:userId, :postId, now())
        """,
        nativeQuery = true,
    )
    fun insertIgnore(
        @Param("userId") userId: Long,
        @Param("postId") postId: Long,
    ): Int

    /**
     * 북마크 취소
     *
     * - 실제 삭제되면 1 반환
     * - 이미 취소된 상태면 0 반환
     */
    @Modifying
    @Query(
        """
            delete from Bookmark b
            where b.member.userId = :userId
            and b.post.postId = :postId
        """
    )
    fun deleteByUserIdAndPostId(
        @Param("userId") userId: Long,
        @Param("postId") postId: Long,
    ): Int
}