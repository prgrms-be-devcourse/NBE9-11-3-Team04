package com.back.devc.domain.interaction.bookmark.repository;

import com.back.devc.domain.interaction.bookmark.entity.Bookmark;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.post.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMemberAndPost(Member member, Post post);

    Optional<Bookmark> findByMemberAndPost(Member member, Post post);

    List<Bookmark> findAllByMember(Member member);

    List<Bookmark> findAllByMemberAndPost_IsDeletedFalse(Member member);

    Page<Bookmark> findAllByMemberAndPost_IsDeletedFalse(Member member, Pageable pageable);

    void deleteByPost_PostId(Long postId);

    /**
     * 현재 로그인한 사용자가 특정 게시글을 북마크했는지 확인
     *
     * 게시글 상세조회 응답에서 bookmarked 값을 내려주기 위해 사용
     */
    boolean existsByMember_UserIdAndPost_PostId(Long userId, Long postId);

    /**
     * 특정 게시글의 전체 북마크 수를 조회
     *
     * 게시글 상세 페이지에서 북마크 수를 바로 표시할 수 있도록
     * 상세조회 응답에 bookmarkCount 를 포함할 때 사용
     */
    long countByPost_PostId(Long postId);

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
            nativeQuery = true
    )
    int insertIgnore(
            @Param("userId") Long userId,
            @Param("postId") Long postId
    );

    /**
     * 북마크 취소
     *
     * - 실제 삭제되면 1 반환
     * - 이미 취소된 상태면 0 반환
     */
    @Modifying
    @Query("""
            delete from Bookmark b
            where b.member.userId = :userId
            and b.post.postId = :postId
            """)
    int deleteByUserIdAndPostId(
            @Param("userId") Long userId,
            @Param("postId") Long postId
    );
}